package com.aishac.customer.controller;

import com.aishac.customer.dto.BlockCustomerRequest;
import com.aishac.customer.dto.CustomerPage;
import com.aishac.customer.dto.CustomerRequest;
import com.aishac.customer.dto.CustomerResponse;
import com.aishac.customer.dto.CustomerUpdateRequest;
import com.aishac.customer.dto.PageResponse;
import com.aishac.customer.dto.ProblemResponse;
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
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Customers", description = "Cadastro e status do cliente")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final CustomerService customerService;

    @Operation(
            operationId = "listCustomers",
            summary = "Listar clientes",
            description = "Busca paginada. name: contém, sem diferenciar maiúsculas. email e cpf: exatos (checkout).")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Página de clientes (vazia se nenhum corresponder)",
                content = @Content(schema = @Schema(implementation = CustomerPage.class))),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou INVALID_CPF",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public PageResponse<CustomerResponse> list(
            @Parameter(description = "Parte do nome") @RequestParam(required = false) @Size(max = 150) String name,
            @Parameter(description = "E-mail exato") @RequestParam(required = false) @Size(max = 254) String email,
            @Parameter(description = "CPF exato, com ou sem pontuação") @RequestParam(required = false) @Size(max = 14)
                    String cpf,
            @RequestParam(required = false) CustomerStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.of(customerService.list(name, email, cpf, status, pageable));
    }

    @Operation(
            operationId = "getCustomer",
            summary = "Buscar cliente por id",
            description = "Funciona para qualquer status, inclusive INACTIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping("/{customerId}")
    public CustomerResponse findById(@PathVariable UUID customerId) {
        return customerService.findById(customerId);
    }

    @Operation(
            operationId = "createCustomer",
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
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CPF_ALREADY_EXISTS ou EMAIL_ALREADY_EXISTS",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.created(location(response.id())).body(response);
    }

    @Operation(
            operationId = "updateCustomer",
            summary = "Atualizar cliente",
            description = "Substitui nome, e-mail e telefone. O CPF não muda. Exige a version lida (lock otimista).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "EMAIL_ALREADY_EXISTS ou CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping("/{customerId}")
    public CustomerResponse update(@PathVariable UUID customerId, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(customerId, request);
    }

    @Operation(
            operationId = "blockCustomer",
            summary = "Bloquear cliente",
            description = "ACTIVE → BLOCKED, com motivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente bloqueado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION ou CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{customerId}/block")
    public CustomerResponse block(@PathVariable UUID customerId, @Valid @RequestBody BlockCustomerRequest request) {
        return customerService.block(customerId, request);
    }

    @Operation(operationId = "unblockCustomer", summary = "Desbloquear cliente", description = "BLOCKED → ACTIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente desbloqueado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION ou CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{customerId}/unblock")
    public CustomerResponse unblock(@PathVariable UUID customerId) {
        return customerService.unblock(customerId);
    }

    @Operation(
            operationId = "deactivateCustomer",
            summary = "Desativar cliente",
            description = "ACTIVE ou BLOCKED → INACTIVE. Não existe exclusão de cliente (ADR-016).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente desativado"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_STATUS_TRANSITION ou CONCURRENT_MODIFICATION",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping("/{customerId}/deactivate")
    public CustomerResponse deactivate(@PathVariable UUID customerId) {
        return customerService.deactivate(customerId);
    }

    private static URI location(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
