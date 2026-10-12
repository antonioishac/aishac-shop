package com.aishac.customer.customer.adapter.in.web;

import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.BAD_REQUEST;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.CUSTOMER_NOT_FOUND;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.DUPLICATED;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.INACTIVE;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.PROBLEM_JSON;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.TRANSITION_CONFLICT;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.UPDATE_CONFLICT;

import com.aishac.customer.customer.adapter.in.web.dto.BlockCustomerRequest;
import com.aishac.customer.customer.adapter.in.web.dto.CreateCustomerRequest;
import com.aishac.customer.customer.adapter.in.web.dto.CustomerPageResponse;
import com.aishac.customer.customer.adapter.in.web.dto.CustomerResponse;
import com.aishac.customer.customer.adapter.in.web.dto.UpdateCustomerRequest;
import com.aishac.customer.customer.application.port.in.CustomerCommands.BlockCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.RegisterCustomer;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateCustomer;
import com.aishac.customer.customer.application.port.in.CustomerQueries;
import com.aishac.customer.customer.application.port.in.ManageCustomerUseCase;
import com.aishac.customer.customer.application.port.out.CustomerFilter;
import com.aishac.customer.customer.domain.Cpf;
import com.aishac.customer.customer.domain.Customer;
import com.aishac.customer.customer.domain.CustomerStatus;
import com.aishac.customer.customer.domain.Email;
import com.aishac.customer.shared.application.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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

@Tag(name = "Customers", description = "Cadastro e status do cliente")
@RestController
@RequestMapping("/api/{version}/customers")
@RequiredArgsConstructor
class CustomerController {

    private final ManageCustomerUseCase manageCustomer;
    private final CustomerQueries queries;
    private final CustomerWebMapper mapper;

    @Operation(
            summary = "Cadastrar cliente",
            description = "Cria o cliente com status ACTIVE. CPF com ou sem pontuação; e-mail normalizado.")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado (Location aponta para ele)")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = DUPLICATED,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(version = "1")
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = manageCustomer.register(
                new RegisterCustomer(request.fullName(), request.cpf(), request.email(), request.phone()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(customer));
    }

    @Operation(
            summary = "Listar clientes",
            description = "Paginado. name: contém, sem diferenciar maiúsculas. email e cpf: exatos (checkout).")
    @ApiResponse(responseCode = "200", description = "Página de clientes (vazia se nenhum corresponder)")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(version = "1")
    CustomerPageResponse list(
            @Parameter(description = "Parte do nome") @RequestParam(required = false) @Size(max = 150) @Nullable
                    String name,
            @Parameter(description = "E-mail exato") @RequestParam(required = false) @Size(max = 254) @Nullable
                    String email,
            @Parameter(description = "CPF exato, com ou sem pontuação")
                    @RequestParam(required = false)
                    @Size(max = 14)
                    @Nullable
                    String cpf,
            @Parameter(description = "Status") @RequestParam(required = false) @Nullable CustomerStatus status,
            @Parameter(description = "Página (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Tamanho (máx. 100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100)
                    int size,
            @Parameter(description = "fullName ou createdAt, com ,asc ou ,desc")
                    @RequestParam(defaultValue = "createdAt,desc")
                    @Pattern(regexp = "(fullName|createdAt)(,(asc|desc))?", message = "must be fullName or createdAt")
                    String sort) {
        String[] sortParts = sort.split(",");
        boolean ascending = sortParts.length < 2 || "asc".equals(sortParts[1]);
        CustomerFilter filter = new CustomerFilter(
                name,
                email == null || email.isBlank() ? null : Email.of(email),
                cpf == null || cpf.isBlank() ? null : Cpf.of(cpf),
                status);
        return mapper.toResponse(queries.search(filter, new PageQuery(page, size, sortParts[0], ascending)));
    }

    @Operation(summary = "Buscar cliente por ID", description = "Funciona para qualquer status, inclusive INACTIVE.")
    @ApiResponse(responseCode = "200", description = "Cliente")
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = "/{customerId}", version = "1")
    CustomerResponse get(@Parameter(description = "ID do cliente") @PathVariable UUID customerId) {
        return mapper.toResponse(queries.get(customerId));
    }

    @Operation(
            summary = "Atualizar dados do cliente",
            description = "Substitui nome, e-mail e telefone. O CPF não muda. Exige a version lida.")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = UPDATE_CONFLICT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = INACTIVE,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = "/{customerId}", version = "1")
    CustomerResponse update(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return mapper.toResponse(manageCustomer.update(new UpdateCustomer(
                customerId, request.fullName(), request.email(), request.phone(), request.version())));
    }

    @Operation(summary = "Bloquear cliente", description = "ACTIVE → BLOCKED, com motivo.")
    @ApiResponse(responseCode = "200", description = "Cliente bloqueado")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = TRANSITION_CONFLICT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{customerId}/block", version = "1")
    CustomerResponse block(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Valid @RequestBody BlockCustomerRequest request) {
        return mapper.toResponse(manageCustomer.block(new BlockCustomer(customerId, request.reason())));
    }

    @Operation(summary = "Desbloquear cliente", description = "BLOCKED → ACTIVE.")
    @ApiResponse(responseCode = "200", description = "Cliente desbloqueado")
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = TRANSITION_CONFLICT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{customerId}/unblock", version = "1")
    CustomerResponse unblock(@Parameter(description = "ID do cliente") @PathVariable UUID customerId) {
        return mapper.toResponse(manageCustomer.unblock(customerId));
    }

    @Operation(
            summary = "Desativar cliente",
            description = "ACTIVE ou BLOCKED → INACTIVE. Não existe exclusão de cliente (ADR-016).")
    @ApiResponse(responseCode = "200", description = "Cliente desativado")
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = TRANSITION_CONFLICT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{customerId}/deactivate", version = "1")
    CustomerResponse deactivate(@Parameter(description = "ID do cliente") @PathVariable UUID customerId) {
        return mapper.toResponse(manageCustomer.deactivate(customerId));
    }
}
