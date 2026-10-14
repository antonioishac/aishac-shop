package com.aishac.sales.catalog.dto;

/**
 * Schemas nomeados do contrato do catálogo ({@code $ref}), para o cliente gerado no BFF ter um tipo único em vez de um
 * schema inline por campo. Registrados em {@code CatalogOpenApiConfiguration}.
 */
public final class CatalogSchemas {

    public static final String MONEY = "#/components/schemas/Money";
    public static final String STOCK_MOVEMENT_TYPE = "#/components/schemas/StockMovementType";

    /** BRL como string decimal, até 2 casas (ADR-014). */
    public static final String MONEY_PATTERN = "\\d{1,17}(\\.\\d{1,2})?";

    public static final String MONEY_MESSAGE = "must be a decimal with up to 2 places";

    private CatalogSchemas() {}
}
