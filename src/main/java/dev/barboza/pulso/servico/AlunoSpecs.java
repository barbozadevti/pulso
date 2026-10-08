package dev.barboza.pulso.servico;

import org.springframework.data.jpa.domain.Specification;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.StatusMatricula;

/**
 * Filtros combináveis (Specification = a API Criteria do JPA em blocos reutilizáveis):
 * cada filtro é opcional e só entra na consulta se foi informado.
 */
public final class AlunoSpecs {

    private AlunoSpecs() {
    }

    /** Traz a unidade junto (join fetch) para listar sem uma consulta por linha. Não se aplica à contagem da página. */
    public static Specification<Aluno> comUnidade() {
        return (aluno, consulta, cb) -> {
            if (consulta.getResultType() != Long.class && consulta.getResultType() != long.class) {
                aluno.fetch("unidade");
            }
            return null;
        };
    }

    public static Specification<Aluno> nomeOuCpfContem(String texto) {
        return (aluno, consulta, cb) -> {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            String like = "%" + texto.trim().toLowerCase() + "%";
            String digitos = Cpf.somenteDigitos(texto);
            var porNome = cb.like(cb.lower(aluno.get("nome")), like);
            if (digitos.isEmpty()) {
                return porNome;
            }
            return cb.or(porNome, cb.like(cb.function("replace", String.class,
                    cb.function("replace", String.class, aluno.get("cpf"), cb.literal("."), cb.literal("")),
                    cb.literal("-"), cb.literal("")), "%" + digitos + "%"));
        };
    }

    public static Specification<Aluno> daUnidade(Long unidadeId) {
        return (aluno, consulta, cb) -> unidadeId == null ? null
                : cb.equal(aluno.get("unidade").get("id"), unidadeId);
    }

    public static Specification<Aluno> doBairro(String bairro) {
        return (aluno, consulta, cb) -> bairro == null || bairro.isBlank() ? null
                : cb.equal(cb.lower(aluno.get("endereco").get("bairro")), bairro.trim().toLowerCase());
    }

    /**
     * Situação da matrícula, com subconsulta: ATIVA e TRANCADA procuram uma matrícula nesse estado;
     * CANCELADA significa "não tem nenhuma matrícula em andamento".
     */
    public static Specification<Aluno> comSituacao(StatusMatricula status) {
        return (aluno, consulta, cb) -> {
            if (status == null) {
                return null;
            }
            var sub = consulta.subquery(Long.class);
            var m = sub.from(Matricula.class);
            sub.select(m.get("id"));
            if (status == StatusMatricula.CANCELADA) {
                sub.where(cb.equal(m.get("aluno"), aluno), cb.notEqual(m.get("status"), StatusMatricula.CANCELADA));
                return cb.not(cb.exists(sub));
            }
            sub.where(cb.equal(m.get("aluno"), aluno), cb.equal(m.get("status"), status));
            return cb.exists(sub);
        };
    }
}
