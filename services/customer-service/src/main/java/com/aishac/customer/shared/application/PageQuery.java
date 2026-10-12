package com.aishac.customer.shared.application;

/** Pedido de página: número (0-based), tamanho e ordenação por uma propriedade do modelo de leitura. */
public record PageQuery(int page, int size, String sortProperty, boolean ascending) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("invalid page request: page=" + page + ", size=" + size);
        }
    }
}
