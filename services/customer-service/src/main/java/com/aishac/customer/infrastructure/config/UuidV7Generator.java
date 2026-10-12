package com.aishac.customer.infrastructure.config;

import com.aishac.customer.shared.application.IdGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UUID versão 7 (RFC 9562): 48 bits de milissegundos do {@link Clock}, versão, variante e 74 bits aleatórios.
 * Ordenável no tempo, o que mantém o índice da PK compacto (ADR-007).
 */
@Component
@RequiredArgsConstructor
class UuidV7Generator implements IdGenerator {

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Override
    public UUID newId() {
        long millis = clock.millis();
        long mostSignificant = (millis << 16) | 0x7000L | random.nextInt(1 << 12);
        long leastSignificant = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
