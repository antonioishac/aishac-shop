package com.aishac.customer.controller;

import com.aishac.customer.dto.BlockCustomerRequest;
import com.aishac.customer.dto.CustomerRequest;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.CustomerUpdateRequest;
import com.aishac.customer.dto.OrderDataResponse;
import com.aishac.customer.dto.PageResponse;
import com.aishac.customer.entity.CustomerStatus;
import com.aishac.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Customers", description = "Cadastro de cliente: dados, status e dados para pedido")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(
            summary = "Listar clientes",
            description = "Busca paginada. name: contém, sem diferenciar maiúsculas. email e cpf: exatos (checkout)."
                    + " sort: fullName ou createdAt.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de clientes (vazia se nenhum corresponder)"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou INVALID_CPF",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public PageResponse<CustomerResponse> list(
            @Parameter(description = "Parte do nome") @RequestParam(required = false) String name,
            @Parameter(description = "E-mail exato") @RequestParam(required = false) String email,
            @Parameter(description = "CPF exato, com ou sem pontuação") @RequestParam(required = false) String cpf,
            @RequestParam(required = false) CustomerStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.of(customerService.list(name, email, cpf, status, pageable));
    }

    @Operation(summary = "Buscar cliente por id", description = "Funciona para qualquer status, inclusive INACTIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return customerService.findById(id);
    }

    @Operation(
            summary = "Cadastrar cliente",
            description = "Nasce ACTIVE. CPF com ou sem pontuação; e-mail normalizado.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Cliente cadastrado",
                headers = @Header(name = "Location", description = "URL do cliente criado")),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou INVALID_CPF",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CPF_ALREADY_EXISTS ou EMAIL_ALREADY_EXISTS",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.created(location(response.id())).body(response);
    }

    @Operation(
            summary = "Atualizar cliente",
            description = "Substitui nome, e-mail e telefone. O CPF não muda. Exige a version lida (lock otimista).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "EMAIL_ALREADY_EXISTS ou CONCURRENT_MODIFICATION",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }

    @Operation(summary = "Bloquear cliente", description = "ACTIVE → BLOCKED, com motivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente bloqueado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/block")
    public CustomerResponse block(@PathVariable UUID id, @Valid @RequestBody BlockCustomerRequest request) {
        return customerService.block(id, request);
    }

    @Operation(summary = "Desbloquear cliente", description = "BLOCKED → ACTIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente desbloqueado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/unblock")
    public CustomerResponse unblock(@PathVariable UUID id) {
        return customerService.unblock(id);
    }

    @Operation(
            summary = "Desativar cliente",
            description = "ACTIVE ou BLOCKED → INACTIVE. Não existe exclusão de cliente (ADR-016).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente desativado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/deactivate")
    public CustomerResponse deactivate(@PathVariable UUID id) {
        return customerService.deactivate(id);
    }

    @Operation(
            summary = "Dados do cliente para criação de pedido",
            description = "Consumido pelo sales-service. Devolve o status real; shippingAddress é nulo se não houver"
                    + " addressId nem endereço padrão.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados para pedido"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}/order-data")
    public OrderDataResponse findOrderData(
            @PathVariable UUID id,
            @Parameter(description = "Endereço de entrega; se omitido, usa o padrão") @RequestParam(required = false)
                    UUID addressId) {
        return customerService.findOrderData(id, addressId);
    }

    private static URI location(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
