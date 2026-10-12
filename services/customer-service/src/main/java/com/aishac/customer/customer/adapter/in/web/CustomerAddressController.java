package com.aishac.customer.customer.adapter.in.web;

import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.ADDRESS_RULE;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.BAD_REQUEST;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.CUSTOMER_NOT_FOUND;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.INACTIVE;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.NOT_FOUND;
import static com.aishac.customer.customer.adapter.in.web.ApiErrorResponses.PROBLEM_JSON;

import com.aishac.customer.customer.adapter.in.web.dto.AddressRequest;
import com.aishac.customer.customer.adapter.in.web.dto.AddressResponse;
import com.aishac.customer.customer.application.port.in.CustomerCommands.AddAddress;
import com.aishac.customer.customer.application.port.in.CustomerCommands.UpdateAddress;
import com.aishac.customer.customer.application.port.in.ManageAddressesUseCase;
import com.aishac.customer.customer.domain.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Addresses", description = "Endereços de entrega do cliente")
@RestController
@RequestMapping("/api/{version}/customers/{customerId}/addresses")
@RequiredArgsConstructor
class CustomerAddressController {

    private final ManageAddressesUseCase addresses;
    private final CustomerWebMapper mapper;

    @Operation(
            summary = "Cadastrar endereço",
            description = "O primeiro endereço vira padrão automaticamente. Máximo de 10 por cliente.")
    @ApiResponse(responseCode = "201", description = "Endereço cadastrado (Location aponta para ele)")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = ADDRESS_RULE,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(version = "1")
    ResponseEntity<AddressResponse> add(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Valid @RequestBody AddressRequest request) {
        Address address = addresses.add(new AddAddress(customerId, mapper.toData(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(address.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(address));
    }

    @Operation(summary = "Listar endereços do cliente", description = "Até 10 itens; o padrão vem primeiro.")
    @ApiResponse(responseCode = "200", description = "Endereços")
    @ApiResponse(
            responseCode = "404",
            description = CUSTOMER_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(version = "1")
    List<AddressResponse> list(@Parameter(description = "ID do cliente") @PathVariable UUID customerId) {
        return addresses.list(customerId).stream().map(mapper::toResponse).toList();
    }

    @Operation(summary = "Atualizar endereço")
    @ApiResponse(responseCode = "200", description = "Endereço atualizado")
    @ApiResponse(
            responseCode = "400",
            description = BAD_REQUEST,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = INACTIVE,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = "/{addressId}", version = "1")
    AddressResponse update(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Parameter(description = "ID do endereço") @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        return mapper.toResponse(addresses.update(new UpdateAddress(customerId, addressId, mapper.toData(request))));
    }

    @Operation(
            summary = "Remover endereço",
            description = "Se for o padrão e houver outros, o cadastrado mais recentemente vira o padrão.")
    @ApiResponse(responseCode = "204", description = "Endereço removido")
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = INACTIVE,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping(path = "/{addressId}", version = "1")
    ResponseEntity<Void> remove(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Parameter(description = "ID do endereço") @PathVariable UUID addressId) {
        addresses.remove(customerId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Definir endereço padrão", description = "Desmarca o padrão anterior. Idempotente.")
    @ApiResponse(responseCode = "204", description = "Endereço definido como padrão")
    @ApiResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = INACTIVE,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(path = "/{addressId}/default", version = "1")
    ResponseEntity<Void> makeDefault(
            @Parameter(description = "ID do cliente") @PathVariable UUID customerId,
            @Parameter(description = "ID do endereço") @PathVariable UUID addressId) {
        addresses.makeDefault(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
