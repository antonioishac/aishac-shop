package com.aishac.sales.catalog.controller;

import com.aishac.sales.catalog.dto.StockMovementPage;
import com.aishac.sales.catalog.dto.StockMovementRequest;
import com.aishac.sales.catalog.dto.StockMovementResponse;
import com.aishac.sales.catalog.service.StockService;
import com.aishac.sales.dto.PageResponse;
import com.aishac.sales.dto.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stock", description = "Movimentações e histórico de estoque")
@RestController
@RequestMapping("/api/v1/products/{productId}/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final StockService stockService;

    @Operation(
            operationId = "createStockMovement",
            summary = "Registrar entrada ou saída de estoque",
            description = "A saída é atômica e nunca deixa o estoque negativo.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movimentação registrada"),
        @ApiResponse(
                responseCode = "400",
                description = "VALIDATION_ERROR ou MALFORMED_REQUEST",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INSUFFICIENT_STOCK",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse create(
            @PathVariable UUID productId, @Valid @RequestBody StockMovementRequest request) {
        return stockService.register(productId, request);
    }

    @Operation(
            operationId = "listStockMovements",
            summary = "Histórico de estoque",
            description = "Da movimentação mais recente para a mais antiga.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Página de movimentações",
                content = @Content(schema = @Schema(implementation = StockMovementPage.class))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemResponse.class)))
    })
    @GetMapping
    public PageResponse<StockMovementResponse> list(
            @PathVariable UUID productId,
            @ParameterObject
                    @PageableDefault(
                            size = 20,
                            sort = {"createdAt", "id"},
                            direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return PageResponse.of(stockService.history(productId, pageable));
    }
}
