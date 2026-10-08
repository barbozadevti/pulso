package dev.barboza.pulso.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Mensalidade;
import dev.barboza.pulso.repositorio.Resumos.Atraso;
import dev.barboza.pulso.repositorio.Resumos.PorMes;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {

    @Query("""
            select m from Mensalidade m
            join fetch m.matricula mt join fetch mt.aluno a join fetch mt.plano
            where m.pagamento is null and m.vencimento < :hoje
              and (:unidadeId is null or a.unidade.id = :unidadeId)
            order by m.vencimento
            """)
    List<Mensalidade> atrasadas(@Param("hoje") LocalDate hoje, @Param("unidadeId") Long unidadeId);

    @Query("""
            select coalesce(sum(m.valor), 0) from Mensalidade m
            where m.pagamento is null and m.vencimento < :hoje
              and (:unidadeId is null or m.matricula.aluno.unidade.id = :unidadeId)
            """)
    BigDecimal valorEmAtraso(@Param("hoje") LocalDate hoje, @Param("unidadeId") Long unidadeId);

    @Query("""
            select count(distinct mt.aluno.id) from Mensalidade m join m.matricula mt
            where m.pagamento is null and m.vencimento < :hoje
              and (:unidadeId is null or mt.aluno.unidade.id = :unidadeId)
            """)
    long alunosInadimplentes(@Param("hoje") LocalDate hoje, @Param("unidadeId") Long unidadeId);

    /** Para cada aluno com mensalidade em atraso, o vencimento mais antigo (base do bloqueio na catraca e do risco). */
    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$Atraso(mt.aluno.id, min(m.vencimento))
            from Mensalidade m join m.matricula mt
            where m.pagamento is null and m.vencimento < :hoje group by mt.aluno.id
            """)
    List<Atraso> vencimentoMaisAntigoPorAluno(@Param("hoje") LocalDate hoje);

    @Query("select min(m.vencimento) from Mensalidade m where m.matricula.aluno.id = :alunoId and m.pagamento is null and m.vencimento < :hoje")
    LocalDate vencimentoMaisAntigoDoAluno(@Param("alunoId") Long alunoId, @Param("hoje") LocalDate hoje);

    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$PorMes(m.competencia, sum(m.valor),
                   coalesce(sum(case when m.pagamento is null then m.valor else 0 end), 0))
            from Mensalidade m
            where m.competencia >= :desde and (:unidadeId is null or m.matricula.aluno.unidade.id = :unidadeId)
            group by m.competencia order by m.competencia
            """)
    List<PorMes> faturamentoPorMes(@Param("desde") LocalDate desde, @Param("unidadeId") Long unidadeId);

    @Query("select m from Mensalidade m left join fetch m.pagamento where m.matricula.aluno.id = :alunoId order by m.competencia desc")
    List<Mensalidade> doAluno(@Param("alunoId") Long alunoId);
}
