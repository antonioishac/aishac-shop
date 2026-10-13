package com.aishac.customer.contract;

import static org.mockito.BDDMockito.given;

import com.aishac.customer.config.ClockConfiguration;
import com.aishac.customer.controller.CustomerOrderDataController;
import com.aishac.customer.dto.AddressResponse;
import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.exception.CustomerErrorCode;
import com.aishac.customer.exception.GlobalExceptionHandler;
import com.aishac.customer.exception.NotFoundException;
import com.aishac.customer.service.CustomerService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base dos testes gerados de {@code contracts/sales-service/} (F01-025): fatia web real (controller, filtro de
 * correlação e tradução de erros) com o service mockado. Os ids abaixo são os mesmos dos contratos.
 */
@WebMvcTest(CustomerOrderDataController.class)
@Import({GlobalExceptionHandler.class, ClockConfiguration.class})
public abstract class SalesServiceBase {

    static final UUID ACTIVE_CUSTOMER = UUID.fromString("0192f0c4-7d1e-7a3b-9c2d-4e5f6a7b8c9d");
    static final UUID CUSTOMER_WITHOUT_ADDRESS = UUID.fromString("0192f0c4-8e2f-7b4c-8d3e-5f6a7b8c9d0e");
    static final UUID BLOCKED_CUSTOMER = UUID.fromString("0192f0c4-9f3a-7c5d-9e4f-6a7b8c9d0e1f");
    static final UUID INACTIVE_CUSTOMER = UUID.fromString("0192f0c4-a04b-7d6e-8f5a-7b8c9d0e1f2a");
    static final UUID UNKNOWN_CUSTOMER = UUID.fromString("0192f0c4-0000-7000-8000-000000000404");
    static final UUID DEFAULT_ADDRESS = UUID.fromString("0192f0c5-1a2b-7c3d-8e4f-5a6b7c8d9e0f");
    static final UUID WORK_ADDRESS = UUID.fromString("0192f0c5-2b3c-7d4e-9f5a-6b7c8d9e0f1a");
    static final UUID UNKNOWN_ADDRESS = UUID.fromString("0192f0c5-0000-7000-8000-000000000404");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CustomerService customerService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mvc);

        AddressResponse home = new AddressResponse(
                DEFAULT_ADDRESS,
                "Casa",
                "01310100",
                "Avenida Paulista",
                "1000",
                "Apto 42",
                "Bela Vista",
                "São Paulo",
                "SP",
                "BR",
                true);
        AddressResponse work = new AddressResponse(
                WORK_ADDRESS,
                "Trabalho",
                "04538133",
                "Avenida Brigadeiro Faria Lima",
                "3477",
                null,
                "Itaim Bibi",
                "São Paulo",
                "SP",
                "BR",
                false);

        given(customerService.findOrderData(ACTIVE_CUSTOMER, null))
                .willReturn(orderData(ACTIVE_CUSTOMER, "Ana Souza", "ana@example.com", CustomerStatus.ACTIVE, home));
        given(customerService.findOrderData(ACTIVE_CUSTOMER, WORK_ADDRESS))
                .willReturn(orderData(ACTIVE_CUSTOMER, "Ana Souza", "ana@example.com", CustomerStatus.ACTIVE, work));
        given(customerService.findOrderData(ACTIVE_CUSTOMER, UNKNOWN_ADDRESS))
                .willThrow(new NotFoundException(CustomerErrorCode.ADDRESS_NOT_FOUND, UNKNOWN_ADDRESS));
        given(customerService.findOrderData(CUSTOMER_WITHOUT_ADDRESS, null))
                .willReturn(orderData(
                        CUSTOMER_WITHOUT_ADDRESS, "Bruno Lima", "bruno@example.com", CustomerStatus.ACTIVE, null));
        given(customerService.findOrderData(BLOCKED_CUSTOMER, null))
                .willReturn(
                        orderData(BLOCKED_CUSTOMER, "Carla Dias", "carla@example.com", CustomerStatus.BLOCKED, home));
        given(customerService.findOrderData(INACTIVE_CUSTOMER, null))
                .willReturn(orderData(
                        INACTIVE_CUSTOMER, "Diego Rocha", "diego@example.com", CustomerStatus.INACTIVE, home));
        given(customerService.findOrderData(UNKNOWN_CUSTOMER, null))
                .willThrow(new NotFoundException(CustomerErrorCode.CUSTOMER_NOT_FOUND, UNKNOWN_CUSTOMER));
    }

    private static OrderDataResponse orderData(
            UUID id, String fullName, String email, CustomerStatus status, AddressResponse address) {
        return new OrderDataResponse(id, fullName, email, status, address);
    }
}
