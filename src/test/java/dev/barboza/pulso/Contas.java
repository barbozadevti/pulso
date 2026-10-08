package dev.barboza.pulso;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.barboza.pulso.seguranca.Perfil;
import dev.barboza.pulso.seguranca.UsuarioLogado;

/** Ajudantes de teste: entrar como um perfil sem passar pelo formulário de login. */
public final class Contas {

    private Contas() {
    }

    public static RequestPostProcessor como(UsuarioLogado u) {
        return authentication(new UsernamePasswordAuthenticationToken(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.perfil().name()))));
    }

    public static UsuarioLogado diretoria() {
        return new UsuarioLogado(1L, "diretoria@pulso.dev", "Marina Prado", Perfil.DIRETORIA, null, null);
    }

    /** Os testes de API de ponta a ponta rodam, por padrão, como a diretoria (vê tudo) e já com o token CSRF. */
    @TestConfiguration
    public static class ComoDiretoria {

        @Bean
        MockMvcBuilderCustomizer entrarComoDiretoria() {
            return builder -> builder.defaultRequest(MockMvcRequestBuilders.get("/").with(como(diretoria())).with(csrf()));
        }
    }
}
