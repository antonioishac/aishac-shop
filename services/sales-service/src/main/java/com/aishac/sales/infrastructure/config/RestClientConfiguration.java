package com.aishac.sales.infrastructure.config;

import com.aishac.sales.infrastructure.web.CorrelationIdPropagationInterceptor;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Todo RestClient criado a partir do builder do Spring propaga o ID de correlação. */
@Configuration(proxyBeanMethods = false)
class RestClientConfiguration {

    @Bean
    RestClientCustomizer correlationIdPropagation() {
        return builder -> builder.requestInterceptor(new CorrelationIdPropagationInterceptor());
    }
}
