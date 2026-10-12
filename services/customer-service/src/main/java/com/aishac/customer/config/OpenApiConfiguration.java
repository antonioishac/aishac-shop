package com.aishac.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
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
