package dev.barboza.pulso.seguranca;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.seguranca.Excecoes.AcessoNegado;
import dev.barboza.pulso.servico.Erros;
import dev.barboza.pulso.servico.Visoes.Ficha;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Regras por DADO: o gerente e a recepção só enxergam a própria unidade; o aluno, só o próprio cadastro.
 * A diretoria passa direto. Quem tenta alcançar um registro de fora do escopo recebe 404 (não 403),
 * para não confirmar que o registro existe.
 */
@Component
@Transactional(readOnly = true)
public class Escopo {

    @PersistenceContext
    private EntityManager em;

    public UsuarioLogado quem() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof UsuarioLogado u)) {
            throw new AcessoNegado("Faça login para continuar.");
        }
        return u;
    }

    /**
     * Unidade efetiva de uma consulta. A diretoria escolhe (ou vê a rede toda se nulo); quem é preso a uma
     * unidade só pode pedir a própria. O aluno enxerga a unidade onde está matriculado.
     */
    public Long unidade(Long pedida) {
        UsuarioLogado u = quem();
        if (u.perfil() == Perfil.DIRETORIA) {
            return pedida;
        }
        Long minha = unidadeDe(u);
        if (pedida != null && !pedida.equals(minha)) {
            throw new Erros.NaoEncontrado("Unidade não encontrada.");
        }
        return minha;
    }

    /** Pode agir sobre este aluno? */
    public void aluno(Long alunoId) {
        UsuarioLogado u = quem();
        if (u.perfil() == Perfil.DIRETORIA) {
            return;
        }
        if (u.perfil() == Perfil.ALUNO) {
            if (!alunoId.equals(u.alunoId())) {
                throw new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado.");
            }
            return;
        }
        exigirUnidade(consulta("select a.unidade.id from Aluno a where a.id = :id", alunoId), u);
    }

    public void matricula(Long id) {
        porUnidade("select m.aluno.unidade.id from Matricula m where m.id = :id", id);
    }

    public void mensalidade(Long id) {
        porUnidade("select m.matricula.aluno.unidade.id from Mensalidade m where m.id = :id", id);
    }

    public void avaliacao(Long id) {
        porUnidade("select a.aluno.unidade.id from AvaliacaoFisica a where a.id = :id", id);
    }

    public void aula(Long id) {
        porUnidade("select a.unidade.id from Aula a where a.id = :id", id);
    }

    /** Reserva: staff só da própria unidade; aluno só a que ele mesmo fez. */
    public void reserva(Long id) {
        UsuarioLogado u = quem();
        if (u.perfil() == Perfil.DIRETORIA) {
            return;
        }
        if (u.perfil() == Perfil.ALUNO) {
            List<Long> donos = em.createQuery("select r.aluno.id from Reserva r where r.id = :id", Long.class)
                    .setParameter("id", id).getResultList();
            if (!donos.isEmpty() && !donos.get(0).equals(u.alunoId())) {
                throw new Erros.NaoEncontrado("Reserva " + id + " não encontrada.");
            }
            return;
        }
        exigirUnidade(consulta("select r.aluno.unidade.id from Reserva r where r.id = :id", id), u);
    }

    /** A ficha para quem pede: risco e anotações de retenção são só da gestão. */
    public Ficha paraOPerfil(Ficha f) {
        if (quem().eh(Perfil.DIRETORIA, Perfil.GERENTE)) {
            return f;
        }
        return new Ficha(f.aluno(), f.nascimento(), f.matriculas(), f.mensalidades(), f.avaliacoes(), f.frequencia(),
                null, f.reservas(), List.of());
    }

    // ---- internos

    private void porUnidade(String jpql, Long id) {
        UsuarioLogado u = quem();
        if (u.perfil() == Perfil.DIRETORIA) {
            return;
        }
        exigirUnidade(consulta(jpql, id), u);
    }

    private Long consulta(String jpql, Long id) {
        List<Long> r = em.createQuery(jpql, Long.class).setParameter("id", id).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** Se o registro não existe, deixa seguir (o serviço responde 404); se é de outra unidade, 404 também. */
    private void exigirUnidade(Long unidadeDoRegistro, UsuarioLogado u) {
        if (unidadeDoRegistro != null && !unidadeDoRegistro.equals(unidadeDe(u))) {
            throw new Erros.NaoEncontrado("Registro não encontrado.");
        }
    }

    private Long unidadeDe(UsuarioLogado u) {
        if (u.perfil() == Perfil.ALUNO) {
            return consulta("select a.unidade.id from Aluno a where a.id = :id", u.alunoId());
        }
        return u.unidadeId();
    }
}
