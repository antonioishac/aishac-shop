package com.aishac.sales.catalog.dto;

import com.aishac.sales.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Só documentação OpenAPI: a forma de {@code PageResponse<CategoryResponse>}, com o nome do contrato. */
@Schema(name = "CategoryPage")
public record CategoryPage(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CategoryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PageResponse.PageMetadata page) {}
