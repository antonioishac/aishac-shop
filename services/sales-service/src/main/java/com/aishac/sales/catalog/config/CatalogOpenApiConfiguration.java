package com.aishac.sales.catalog.config;

import com.aishac.sales.catalog.dto.CatalogSchemas;
import com.aishac.sales.catalog.entity.StockMovementType;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogOpenApiConfiguration {

    /** Schemas nomeados do contrato do catálogo (ver {@link CatalogSchemas}). */
    @Bean
    public OpenApiCustomizer catalogSchemas() {
        return openApi -> openApi.getComponents()
                .addSchemas(
                        "Money",
                        new StringSchema()
                                .pattern("^" + CatalogSchemas.MONEY_PATTERN + "$")
                                .description("Valor em BRL, string decimal com até 2 casas")
                                .example("299.90"))
                .addSchemas(
                        "StockMovementType",
                        new StringSchema()
                                ._enum(Arrays.stream(StockMovementType.values())
                                        .map(Enum::name)
                                        .toList()));
    }
}
