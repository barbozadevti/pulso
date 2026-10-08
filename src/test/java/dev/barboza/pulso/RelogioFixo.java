package dev.barboza.pulso;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Quarta-feira, 7/10/2026, 14h em Brasília: os dados de demonstração e as regras de tempo ficam estáveis. */
@TestConfiguration
public class RelogioFixo {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Bean
    @Primary
    Clock relogioFixo() {
        return Clock.fixed(Instant.parse("2026-10-07T17:00:00Z"), ZONA);
    }
}
