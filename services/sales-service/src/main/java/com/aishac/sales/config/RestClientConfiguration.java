package com.aishac.sales.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Todo RestClient criado a partir do builder do Spring propaga o ID de correlação. */
@Configuration
public class RestClientConfiguration {

    @Bean
    RestClientCustomizer correlationIdPropagation() {
        return builder -> builder.requestInterceptor(new CorrelationIdPropagationInterceptor());
    }
}
