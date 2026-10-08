package dev.barboza.pulso.seguranca;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Sessão HTTP (cookie HttpOnly + SameSite) com CSRF no padrão de SPA: o token vai num cookie legível pelo
 * JavaScript e volta no cabeçalho X-XSRF-TOKEN. Aqui ficam as regras por PERFIL (quem pode chamar o quê);
 * as regras por DADO (só a minha unidade, só o meu cadastro) ficam em {@link Escopo}.
 */
@Configuration
public class SegurancaConfig {

    private static final String CSP = String.join("; ",
            "default-src 'self'", "script-src 'self'", "style-src 'self' 'unsafe-inline'", "img-src 'self' data:",
            "font-src 'self'", "connect-src 'self'", "object-src 'none'", "base-uri 'self'", "form-action 'self'",
            "frame-ancestors 'none'");

    private static final String DIRETORIA = Perfil.DIRETORIA.name();
    private static final String GERENTE = Perfil.GERENTE.name();
    private static final String RECEPCAO = Perfil.RECEPCAO.name();

    @Bean
    SecurityFilterChain seguranca(HttpSecurity http, SecurityContextRepository contexto) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        // público: a casca do site, a documentação e o login
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/img/**", "/manifest.webmanifest",
                                "/sw.js", "/health", "/health/**", "/api/auth/entrar", "/api/auth/csrf",
                                "/api/auth/demo", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // só gestão (diretoria e gerente)
                        .requestMatchers("/api/painel", "/api/risco", "/api/risco.csv", "/api/laboratorio/**",
                                "/api/rastro/**").hasAnyRole(DIRETORIA, GERENTE)
                        .requestMatchers(HttpMethod.POST, "/api/alunos/*/contatos", "/api/matriculas/*/*")
                        .hasAnyRole(DIRETORIA, GERENTE)
                        // equipe da unidade (diretoria, gerente e recepção)
                        .requestMatchers(HttpMethod.GET, "/api/alunos").hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        .requestMatchers(HttpMethod.POST, "/api/alunos", "/api/alunos/*/avaliacoes", "/api/matriculas")
                        .hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        .requestMatchers(HttpMethod.PUT, "/api/alunos/*", "/api/avaliacoes/*")
                        .hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        .requestMatchers(HttpMethod.DELETE, "/api/avaliacoes/*")
                        .hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        .requestMatchers(HttpMethod.GET, "/api/matriculas").hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        .requestMatchers("/api/mensalidades/**", "/api/catraca/**")
                        .hasAnyRole(DIRETORIA, GERENTE, RECEPCAO)
                        // o resto exige estar logado (aluno inclusive): ficha própria, aulas, referências
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.spa())
                .securityContext(s -> s.securityContextRepository(contexto))
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                problema(res, HttpStatus.UNAUTHORIZED, "Não autenticado", "Faça login para continuar."))
                        .accessDeniedHandler((req, res, ex) ->
                                problema(res, HttpStatus.FORBIDDEN, "Acesso negado",
                                        "Seu perfil não permite esta operação (ou a sessão expirou: recarregue a página).")))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=()"))
                        .addHeaderWriter((req, res) -> {
                            if (req.getRequestURI().startsWith("/api/")) {
                                res.setHeader("Cache-Control", "no-store");
                            }
                        }));
        return http.build();
    }

    private static void problema(HttpServletResponse res, HttpStatus status, String titulo, String detalhe)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"title\":\"" + titulo + "\",\"status\":" + status.value() + ",\"detail\":\"" + detalhe + "\"}");
    }
}
