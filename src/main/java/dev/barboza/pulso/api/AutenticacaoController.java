package dev.barboza.pulso.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.seguranca.AutenticacaoService;
import dev.barboza.pulso.seguranca.Perfil;
import dev.barboza.pulso.seguranca.UsuarioLogado;
import dev.barboza.pulso.seguranca.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login por sessão (cookie HttpOnly) com proteção CSRF")
public class AutenticacaoController {

    /** Senha das contas de demonstração (dados fictícios). Só é exibida quando pulso.demo=true. */
    public static final String SENHA_DE_DEMONSTRACAO = "Pulso@2026";

    private final AutenticacaoService autenticacao;
    private final SecurityContextRepository contextos;
    private final UsuarioRepository usuarios;
    private final boolean demo;

    public AutenticacaoController(AutenticacaoService autenticacao, SecurityContextRepository contextos,
            UsuarioRepository usuarios, @Value("${pulso.demo:false}") boolean demo) {
        this.autenticacao = autenticacao;
        this.contextos = contextos;
        this.usuarios = usuarios;
        this.demo = demo;
    }

    public record Credenciais(@NotBlank(message = "Informe o e-mail.") String login,
            @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record Sessao(String nome, String login, String perfil, String rotuloDoPerfil, Long unidadeId,
            Long alunoId) {
        static Sessao de(UsuarioLogado u) {
            return new Sessao(u.nome(), u.login(), u.perfil().name(), u.perfil().nome(), u.unidadeId(), u.alunoId());
        }
    }

    public record ContaDeDemonstracao(String perfil, String rotulo, String nome, String login, String senha) {
    }

    @Operation(summary = "Entrar", description = "5 senhas erradas seguidas bloqueiam a conta por 10 minutos.")
    @PostMapping("/entrar")
    public Sessao entrar(@Valid @RequestBody Credenciais c, HttpServletRequest req, HttpServletResponse res) {
        UsuarioLogado u = autenticacao.autenticar(c.login(), c.senha());
        var token = UsernamePasswordAuthenticationToken.authenticated(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.perfil().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(token);
        SecurityContextHolder.setContext(contexto);
        req.getSession(true);
        req.changeSessionId();
        contextos.saveContext(contexto, req, res);
        return Sessao.de(u);
    }

    @Operation(summary = "Quem está logado")
    @GetMapping("/eu")
    public Sessao eu(@AuthenticationPrincipal UsuarioLogado u) {
        return Sessao.de(u);
    }

    @Operation(summary = "Sair", description = "Invalida a sessão.")
    @PostMapping("/sair")
    public ResponseEntity<Void> sair(HttpServletRequest req) {
        var sessao = req.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Garante o cookie XSRF-TOKEN antes do primeiro POST (o site chama ao abrir). */
    @Operation(summary = "Token CSRF")
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    /** Contas de exemplo para quem está avaliando o projeto. Vazio fora do modo de demonstração. */
    @Operation(summary = "Contas de demonstração", description = "Só existe com pulso.demo=true; todos os dados são fictícios.")
    @GetMapping("/demo")
    public List<ContaDeDemonstracao> demo() {
        if (!demo) {
            return List.of();
        }
        return usuarios.findAll().stream()
                .sorted(java.util.Comparator.comparing(u -> u.getPerfil().ordinal()))
                .map(u -> new ContaDeDemonstracao(u.getPerfil().name(), u.getPerfil().nome(), u.getNome(), u.getLogin(),
                        SENHA_DE_DEMONSTRACAO))
                .toList();
    }
}
