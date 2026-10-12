package com.aishac.sales.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fonte de tempo do serviço: injete {@link Clock} em vez de chamar {@code Instant.now()}. Precisão de
 * microssegundos, a mesma do {@code TIMESTAMPTZ}: o que a API devolve é exatamente o que o banco guardou.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
