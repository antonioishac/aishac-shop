package com.aishac.customer.customer.adapter.in.web;

import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.NOT_FOUND;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.PROBLEM_JSON;

import com.aishac.customer.customer.adapter.in.web.dto.OrderDataResponse;
import com.aishac.customer.customer.application.port.in.CustomerQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Contrato consumido pelo sales-service (F01-024; teste de contrato produtor no F01-025). */
@Tag(name = "Order data", description = "Dados do cliente consumidos pelo sales-service")
@RestController
@RequestMapping("/api/{version}/customers/{customerId}/order-data")
@RequiredArgsConstructor
class OrderDataController {

    private final CustomerQueries queries;
    private final CustomerWebMapper mapper;

    @Operation(
            summary = "Dados do cliente para criação de pedido",
            description = "Devolve o status real; quem recusa o pedido é o sales-service. shippingAddress é nulo se"
                    + " não houver addressId nem endereço padrão.")
    @ApiResponse(responseCode = "200", description = "Dados para pedido")
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(version = "1")
    OrderDataResponse get(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Parameter(description = "Endereço de entrega; se omitido, usa o padrão")
                    @RequestParam(required = false)
                    @Nullable
                    UUID addressId) {
        return mapper.toResponse(queries.orderData(customerId, addressId));
    }
}
