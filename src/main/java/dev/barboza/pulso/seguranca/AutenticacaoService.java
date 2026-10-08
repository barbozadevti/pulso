package dev.barboza.pulso.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import dev.barboza.pulso.seguranca.Excecoes.CredenciaisInvalidas;
import dev.barboza.pulso.seguranca.Excecoes.UsuarioBloqueado;

/**
 * Login com bloqueio após 5 senhas erradas seguidas. "Usuário não existe" e "senha errada" respondem igual
 * (e gastam o mesmo tempo de hash), para não revelar quem tem conta.
 */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder senhas, TransactionTemplate transacao,
            Clock relogio) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.transacao = transacao;
        this.relogio = relogio;
        this.hashFalso = senhas.encode("senha-que-ninguem-usa-2026");
    }

    /** Desfecho decidido dentro da transação; as exceções saem só depois do commit (o contador de falhas persiste). */
    private record Tentativa(UsuarioLogado usuario, long minutosBloqueado) {
    }

    public UsuarioLogado autenticar(String login, String senha) {
        String texto = login == null ? "" : login.trim().toLowerCase();
        String informada = senha == null ? "" : senha;
        LocalDateTime agora = LocalDateTime.now(relogio);

        Tentativa t = transacao.execute(status -> {
            Optional<Usuario> achado = usuarios.findByLogin(texto);
            if (achado.isEmpty() || !achado.get().isAtivo()) {
                senhas.matches(informada, hashFalso);
                return new Tentativa(null, 0);
            }
            Usuario u = achado.get();
            if (u.bloqueadoEm(agora)) {
                return new Tentativa(null, Math.max(1, Duration.between(agora, u.getBloqueadoAte()).toMinutes() + 1));
            }
            if (!senhas.matches(informada, u.getSenhaHash())) {
                boolean bloqueou = u.registrarFalha(agora);
                return new Tentativa(null, bloqueou ? Usuario.TEMPO_DE_BLOQUEIO.toMinutes() : 0);
            }
            u.registrarSucesso();
            return new Tentativa(u.comoLogado(), 0);
        });

        if (t.minutosBloqueado() > 0) {
            throw new UsuarioBloqueado(t.minutosBloqueado());
        }
        if (t.usuario() == null) {
            throw new CredenciaisInvalidas();
        }
        return t.usuario();
    }
}
