package dev.barboza.pulso.seguranca;

import java.time.Duration;
import java.time.LocalDateTime;

import dev.barboza.pulso.dominio.Auditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario extends Auditavel {

    public static final int TENTATIVAS_ANTES_DO_BLOQUEIO = 5;
    public static final Duration TEMPO_DE_BLOQUEIO = Duration.ofMinutes(10);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String login;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Perfil perfil;

    @Column(name = "unidade_id")
    private Long unidadeId;

    @Column(name = "aluno_id")
    private Long alunoId;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Usuario() {
    }

    public Usuario(String login, String nome, String senhaHash, Perfil perfil, Long unidadeId, Long alunoId) {
        this.login = login.trim().toLowerCase();
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.unidadeId = unidadeId;
        this.alunoId = alunoId;
    }

    public boolean bloqueadoEm(LocalDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    /** Conta a falha e bloqueia na quinta seguida. Devolve true se acabou de bloquear. */
    public boolean registrarFalha(LocalDateTime agora) {
        tentativasFalhas++;
        if (tentativasFalhas >= TENTATIVAS_ANTES_DO_BLOQUEIO) {
            bloqueadoAte = agora.plus(TEMPO_DE_BLOQUEIO);
            tentativasFalhas = 0;
            return true;
        }
        return false;
    }

    public void registrarSucesso() {
        tentativasFalhas = 0;
        bloqueadoAte = null;
    }

    public UsuarioLogado comoLogado() {
        return new UsuarioLogado(id, login, nome, perfil, unidadeId, alunoId);
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public Long getUnidadeId() { return unidadeId; }
    public Long getAlunoId() { return alunoId; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
}
