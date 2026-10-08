package dev.barboza.pulso.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Acesso;
import dev.barboza.pulso.repositorio.Resumos.Frequencia;
import dev.barboza.pulso.repositorio.Resumos.Contagem;

public interface AcessoRepository extends JpaRepository<Acesso, Long> {

    List<Acesso> findBySaidaIsNull();

    long countByUnidadeIdAndSaidaIsNull(Long unidadeId);

    Optional<Acesso> findFirstByAlunoIdAndSaidaIsNullOrderByEntradaDesc(Long alunoId);

    @Query("select a from Acesso a join fetch a.aluno where a.unidade.id = :unidadeId and a.saida is null order by a.entrada desc")
    List<Acesso> dentroDaUnidade(@Param("unidadeId") Long unidadeId);

    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$Contagem(a.unidade.id, count(a))
            from Acesso a where a.saida is null group by a.unidade.id
            """)
    List<Contagem> dentroPorUnidade();

    @Query("select count(a) from Acesso a where a.entrada >= :desde and (:unidadeId is null or a.unidade.id = :unidadeId)")
    long contarEntradasDesde(@Param("desde") LocalDateTime desde, @Param("unidadeId") Long unidadeId);

    /** Quem está há tempo demais "dentro": a saída nunca foi registrada (esqueceram de passar na catraca). */
    @Query("select a from Acesso a where a.saida is null and a.entrada < :limite")
    List<Acesso> esquecidosAntesDe(@Param("limite") LocalDateTime limite);

    /** Frequência por aluno em janelas de 14 dias, para o risco de evasão. Uma consulta para a base toda. */
    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$Frequencia(a.aluno.id, max(a.entrada),
                   sum(case when a.entrada >= :quinzeDias then 1L else 0L end),
                   sum(case when a.entrada < :quinzeDias then 1L else 0L end))
            from Acesso a where a.entrada >= :vinteEOitoDias group by a.aluno.id
            """)
    List<Frequencia> frequenciaPorAluno(@Param("quinzeDias") LocalDateTime quinzeDias,
            @Param("vinteEOitoDias") LocalDateTime vinteEOitoDias);

    @Query("select a.entrada from Acesso a where a.entrada >= :desde and (:unidadeId is null or a.unidade.id = :unidadeId)")
    List<LocalDateTime> entradasDesde(@Param("desde") LocalDateTime desde, @Param("unidadeId") Long unidadeId);

    @Query("select count(a) from Acesso a where a.aluno.id = :alunoId and a.entrada >= :desde")
    long visitasDoAlunoDesde(@Param("alunoId") Long alunoId, @Param("desde") LocalDateTime desde);

    @Query("select max(a.entrada) from Acesso a where a.aluno.id = :alunoId")
    LocalDateTime ultimaVisitaDoAluno(@Param("alunoId") Long alunoId);

    @Query("select a.entrada from Acesso a where a.aluno.id = :alunoId and a.entrada >= :desde order by a.entrada")
    List<LocalDateTime> visitasDoAluno(@Param("alunoId") Long alunoId, @Param("desde") LocalDateTime desde);
}
