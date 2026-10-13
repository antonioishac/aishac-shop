package com.aishac.customer.controller;

import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.dto.ProblemResponse;
import com.aishac.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contrato consumido pelo sales-service (F01-024; teste de contrato produtor no F01-025). */
@Tag(name = "Order data", description = "Dados do cliente consumidos pelo sales-service")
@RestController
@RequestMapping("/api/v1/customers/{customerId}/order-data")
@RequiredArgsConstructor
public class CustomerOrderDataController {

    private final CustomerService customerService;

    @Operation(
            operationId = "getOrderData",
            summary = "Dados do cliente para criação de pedido",
            description = "Devolve o status real (quem recusa o pedido é o sales-service). shippingAddress é nulo se"
                    + " não houver addressId nem endereço padrão.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados para pedido"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public OrderDataResponse findOrderData(
            @PathVariable UUID customerId,
            @Parameter(description = "Endereço de entrega; se omitido, usa o padrão") @RequestParam(required = false)
                    UUID addressId) {
        return customerService.findOrderData(customerId, addressId);
    }
}
