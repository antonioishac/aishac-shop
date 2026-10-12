package com.aishac.customer.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aishac.customer.config.ClockConfiguration;
import com.aishac.customer.exception.BusinessException;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.GlobalExceptionHandler;
import com.aishac.customer.exception.NotFoundException;
import com.aishac.customer.service.CustomerAddressService;
import com.aishac.customer.service.CustomerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Fatia web: validação de entrada (CA-05, CA-19) e tradução das exceções em ProblemDetail, sem banco. */
@WebMvcTest({CustomerController.class, CustomerAddressController.class})
@Import({GlobalExceptionHandler.class, ClockConfiguration.class})
class CustomerControllerTest {

    private static final String CUSTOMER = "/api/v1/customers/" + UUID.randomUUID();
    private static final String VALID_ADDRESS = "{\"label\":\"Casa\",\"zipCode\":\"01310-100\",\"street\":\"Rua\","
            + "\"number\":\"1\",\"district\":\"Centro\",\"city\":\"SP\",\"state\":\"SP\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    @MockitoBean
    CustomerAddressService customerAddressService;

    // CA-05
    @Test
    void shouldListEveryInvalidFieldOnCreate() throws Exception {
        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"cpf\":\"123\",\"email\":\"not-an-email\",\"phone\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("fullName", "cpf", "email", "phone")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldRequireVersionOnUpdate() throws Exception {
        mvc.perform(put(CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana Souza\",\"email\":\"ana@x.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
    }

    // CA-19
    @Test
    void shouldValidateAddressFields() throws Exception {
        mvc.perform(post(CUSTOMER + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ADDRESS.replace("01310-100", "1310-100").replace("\"SP\"}", "\"XX\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("zipCode", "state")));
    }

    @Test
    void shouldRejectMalformedIdsUnknownStatusAndMalformedBody() throws Exception {
        mvc.perform(get("/api/v1/customers/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
        mvc.perform(get("/api/v1/customers?status=SLEEPING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void shouldTranslateServiceExceptionsUsingTheCatalogStatus() throws Exception {
        given(customerService.deactivate(any()))
                .willThrow(new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, UUID.randomUUID()));
        mvc.perform(post(CUSTOMER + "/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));

        given(customerAddressService.add(any(), any()))
                .willThrow(new BusinessException(CustomerErrorCode.ADDRESS_LIMIT_EXCEEDED));
        mvc.perform(post(CUSTOMER + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ADDRESS))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ADDRESS_LIMIT_EXCEEDED"));

        given(customerService.unblock(any()))
                .willThrow(new BusinessException(CustomerErrorCode.INVALID_STATUS_TRANSITION));
        mvc.perform(post(CUSTOMER + "/unblock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void shouldRejectBlankBlockReason() throws Exception {
        mvc.perform(post(CUSTOMER + "/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }
}
