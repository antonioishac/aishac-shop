package com.aishac.customer.shared.application;

import java.util.List;
import java.util.function.Function;

/** Página de resultados independente de framework. */
public record PageResult<T>(List<T> content, int number, int size, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), number, size, totalElements, totalPages);
    }
}
