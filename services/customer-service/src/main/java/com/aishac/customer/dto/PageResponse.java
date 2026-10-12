package com.aishac.customer.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Envelope de paginação da API: desacopla o contrato público do {@link Page} do Spring Data. */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
