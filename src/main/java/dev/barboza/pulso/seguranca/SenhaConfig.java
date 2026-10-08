package dev.barboza.pulso.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SenhaConfig {

    /** BCrypt por padrão, com prefixo {id} para permitir trocar de algoritmo no futuro. */
    @Bean
    PasswordEncoder codificadorDeSenha() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Onde a sessão guarda quem está logado. */
    @Bean
    SecurityContextRepository repositorioDeContexto() {
        return new HttpSessionSecurityContextRepository();
    }
}
