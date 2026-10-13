package com.aishac.customer.controller;

import com.aishac.customer.dto.AddressRequest;
import com.aishac.customer.dto.AddressResponse;
import com.aishac.customer.dto.ProblemResponse;
import com.aishac.customer.service.CustomerAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Addresses", description = "Endereços de entrega do cliente (até 10, um padrão)")
@RestController
@RequestMapping("/api/v1/customers/{customerId}/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final CustomerAddressService customerAddressService;

    @Operation(
            operationId = "listAddresses",
            summary = "Listar endereços",
            description = "Até 10 itens; o padrão vem primeiro.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Endereços do cliente"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public List<AddressResponse> list(@PathVariable UUID customerId) {
        return customerAddressService.list(customerId);
    }

    @Operation(
            operationId = "addAddress",
            summary = "Cadastrar endereço",
            description = "O primeiro endereço vira padrão automaticamente.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Endereço cadastrado",
                headers = @Header(name = "Location", description = "URL do endereço criado")),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE ou ADDRESS_LIMIT_EXCEEDED",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AddressResponse> add(
            @PathVariable UUID customerId, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = customerAddressService.add(customerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "updateAddress", summary = "Atualizar endereço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Endereço atualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping("/{addressId}")
    public AddressResponse update(
            @PathVariable UUID customerId, @PathVariable UUID addressId, @Valid @RequestBody AddressRequest request) {
        return customerAddressService.update(customerId, addressId, request);
    }

    @Operation(
            operationId = "removeAddress",
            summary = "Remover endereço",
            description = "Se for o padrão e houver outros, o cadastrado mais recentemente vira o padrão.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Endereço removido"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> remove(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        customerAddressService.remove(customerId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "setDefaultAddress",
            summary = "Definir endereço padrão",
            description = "Desmarca o padrão anterior. Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Endereço definido como padrão"),
        @ApiResponse(
                responseCode = "404",
                description = "CUSTOMER_NOT_FOUND ou ADDRESS_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "CUSTOMER_INACTIVE",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PutMapping("/{addressId}/default")
    public ResponseEntity<Void> setDefault(@PathVariable UUID customerId, @PathVariable UUID addressId) {
        customerAddressService.setDefault(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
