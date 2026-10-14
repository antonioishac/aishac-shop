package com.aishac.sales.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Cada caminho do handler central vira ProblemDetail com o código e o status certos, sem subir o Spring. */
class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
            .build();

    @Test
    void shouldUseTheStatusAndCodeOfTheCatalog() throws Exception {
        mvc.perform(get("/fail/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.timestamp").value(NOW.toString()));
        mvc.perform(get("/fail/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }

    @Test
    void shouldTranslateDatabaseErrors() throws Exception {
        mvc.perform(get("/fail/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
        mvc.perform(get("/fail/pessimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
        mvc.perform(get("/fail/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("request"));
        mvc.perform(get("/fail/unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    void shouldTranslateClientErrors() throws Exception {
        mvc.perform(get("/fail/sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
        mvc.perform(get("/fail/id/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
        mvc.perform(post("/fail/body").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(post("/fail/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void shouldHideDetailsOfUnexpectedErrors() throws Exception {
        mvc.perform(get("/fail/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @RestController
    static class FailingController {

        @GetMapping("/fail/business")
        void business() {
            throw new BusinessException(CommonErrorCode.CONCURRENT_MODIFICATION);
        }

        @GetMapping("/fail/not-found")
        void notFound() {
            throw new NotFoundException(CommonErrorCode.ROUTE_NOT_FOUND, UUID.randomUUID());
        }

        @GetMapping("/fail/optimistic-lock")
        void optimisticLock() {
            throw new OptimisticLockingFailureException("stale");
        }

        @GetMapping("/fail/check")
        void check() {
            throw new DataIntegrityViolationException(
                    "check", new ConstraintViolationException("check", new SQLException(), "ck_orders_total"));
        }

        @GetMapping("/fail/unique")
        void unique() {
            throw new DataIntegrityViolationException(
                    "unique", new ConstraintViolationException("unique", new SQLException(), "uk_orders_number"));
        }

        @GetMapping("/fail/pessimistic-lock")
        void pessimisticLock() {
            throw new PessimisticLockingFailureException("lock timeout");
        }

        @GetMapping("/fail/sort")
        void sort() {
            throw new PropertyReferenceException("unknown", TypeInformation.of(String.class), List.of());
        }

        @GetMapping("/fail/id/{id}")
        void typed(@PathVariable UUID id) {}

        @PostMapping("/fail/body")
        void body(@RequestBody Payload payload) {}

        @GetMapping("/fail/unexpected")
        void unexpected() {
            throw new IllegalStateException("internal detail");
        }
    }

    record Payload(String name) {}
}
