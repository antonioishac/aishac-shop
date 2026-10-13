package com.aishac.customer.config;

import com.aishac.customer.dto.OpenApiSchemas;
import com.aishac.customer.entity.CustomerStatus;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customerServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Service API")
                        .version("v1")
                        .description("Clientes, status e endereços do Aishac Shop."));
    }

    /** Enums do contrato como schemas nomeados (ver {@link OpenApiSchemas}). */
    @Bean
    public OpenApiCustomizer contractEnums() {
        return openApi -> openApi.getComponents()
                .addSchemas(
                        "CustomerStatus",
                        new StringSchema()
                                ._enum(Arrays.stream(CustomerStatus.values())
                                        .map(Enum::name)
                                        .toList()))
                .addSchemas("BrazilianState", new StringSchema()._enum(OpenApiSchemas.BRAZILIAN_STATES));
    }

    /** Toda operação aceita e devolve o ID de correlação (ver {@link CorrelationIdFilter}). */
    @Bean
    public OperationCustomizer correlationIdHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name(CorrelationIdFilter.HEADER)
                .required(false)
                .description("ID de correlação; gerado se ausente e devolvido na resposta.")
                .schema(new StringSchema().pattern("^[A-Za-z0-9-]{1,64}$")));
    }
}
