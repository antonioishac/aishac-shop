package com.aishac.customer.customer.adapter.in.web;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aishac.customer.customer.application.port.in.CustomerQueries;
import com.aishac.customer.customer.application.port.in.ManageAddressesUseCase;
import com.aishac.customer.customer.application.port.in.ManageCustomerUseCase;
import com.aishac.customer.customer.domain.CustomerException;
import com.aishac.customer.infrastructure.web.GlobalExceptionHandler;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Fatia web: validação de entrada (CA-05, CA-19) e tradução de erros para ProblemDetail, sem banco. */
@WebMvcTest({CustomerController.class, CustomerAddressController.class, OrderDataController.class})
@Import({GlobalExceptionHandler.class, CustomerControllerTest.Config.class})
class CustomerControllerTest {

    private static final String CUSTOMER = "/api/v1/customers/" + UUID.randomUUID();

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        CustomerWebMapper customerWebMapper() {
            return Mappers.getMapper(CustomerWebMapper.class);
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManageCustomerUseCase manageCustomer;

    @MockitoBean
    ManageAddressesUseCase addresses;

    @MockitoBean
    CustomerQueries queries;

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
                        .content("{\"label\":\"Casa\",\"zipCode\":\"1310-100\",\"street\":\"Rua\",\"number\":\"1\","
                                + "\"district\":\"Centro\",\"city\":\"SP\",\"state\":\"XX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldRejectMalformedIdsAndUnknownStatus() throws Exception {
        mvc.perform(get("/api/v1/customers/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("customerId"));
        mvc.perform(get("/api/v1/customers?status=SLEEPING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldTranslateDomainErrorsByCategory() throws Exception {
        given(manageCustomer.deactivate(any())).willThrow(CustomerException.customerNotFound(UUID.randomUUID()));
        mvc.perform(post(CUSTOMER + "/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));

        given(addresses.add(any())).willThrow(CustomerException.addressLimitExceeded(10));
        mvc.perform(post(CUSTOMER + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Casa\",\"zipCode\":\"01310-100\",\"street\":\"Rua\",\"number\":\"1\","
                                + "\"district\":\"Centro\",\"city\":\"SP\",\"state\":\"SP\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ADDRESS_LIMIT_EXCEEDED"));

        mvc.perform(post(CUSTOMER + "/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void shouldRejectInvalidCpfFilter() throws Exception {
        mvc.perform(get("/api/v1/customers?cpf=111.111.111-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CPF"));
    }
}
