package com.aishac.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** Envelope de paginação da API: desacopla o contrato público do {@link Page} do Spring Data. */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    public record PageMetadata(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int number,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int size,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            long totalElements,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            int totalPages) {}

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
