package com.aishac.customer.dto;

import java.util.List;

/**
 * Enums do contrato publicados como schemas nomeados ({@code $ref}), para o cliente gerado no BFF e no frontend ter um
 * tipo único em vez de um enum inline por campo. Registrados em {@code OpenApiConfiguration}.
 */
public final class OpenApiSchemas {

    public static final String CUSTOMER_STATUS = "#/components/schemas/CustomerStatus";
    public static final String BRAZILIAN_STATE = "#/components/schemas/BrazilianState";

    public static final List<String> BRAZILIAN_STATES = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private OpenApiSchemas() {}
}
