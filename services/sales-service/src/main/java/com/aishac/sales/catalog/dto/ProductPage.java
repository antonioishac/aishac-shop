package com.aishac.sales.catalog.dto;

import com.aishac.sales.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Só documentação OpenAPI: a forma de {@code PageResponse<ProductResponse>}, com o nome do contrato. */
@Schema(name = "ProductPage")
public record ProductPage(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProductResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageResponse.PageMetadata page) {}
