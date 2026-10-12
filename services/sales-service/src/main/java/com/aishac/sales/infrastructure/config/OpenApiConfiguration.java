package com.aishac.sales.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI salesServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sales Service API")
                        .version("v1")
                        .description("Catálogo, pedidos e pagamentos do Aishac Shop."));
    }
}
