package dev.barboza.pulso.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dataHoraDeAuditoria")
public class JpaConfig {

    /** Relógio injetável: os testes trocam por um relógio fixo. Horário de Brasília para a rede toda. */
    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    /** A auditoria (criado_em / atualizado_em) usa o mesmo relógio do restante do sistema. */
    @Bean
    DateTimeProvider dataHoraDeAuditoria(Clock relogio) {
        return () -> Optional.<TemporalAccessor>of(LocalDateTime.now(relogio));
    }
}
