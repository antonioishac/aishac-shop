package com.aishac.sales.catalog.dto;

import com.aishac.sales.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Só documentação OpenAPI: a forma de {@code PageResponse<StockMovementResponse>}, com o nome do contrato. */
@Schema(name = "StockMovementPage")
public record StockMovementPage(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StockMovementResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageResponse.PageMetadata page) {}
