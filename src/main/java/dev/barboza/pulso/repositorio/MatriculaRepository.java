package dev.barboza.pulso.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.repositorio.Resumos.LinhaDaMatricula;
import dev.barboza.pulso.repositorio.Resumos.PorPlano;
import dev.barboza.pulso.repositorio.Resumos.PorUnidade;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    long countByStatus(StatusMatricula status);

    /** Matrícula que ainda vale (não cancelada) do aluno. */
    @Query("select m from Matricula m join fetch m.plano where m.aluno.id = :alunoId and m.status <> 'CANCELADA'")
    Optional<Matricula> buscarVigente(@Param("alunoId") Long alunoId);

    @Query("select m from Matricula m join fetch m.plano where m.aluno.id in :ids and m.status <> 'CANCELADA'")
    List<Matricula> vigentesDosAlunos(@Param("ids") Collection<Long> ids);

    @Query("select m from Matricula m join fetch m.plano where m.aluno.id = :alunoId order by m.inicio desc, m.id desc")
    List<Matricula> doAluno(@Param("alunoId") Long alunoId);

    /** Para o risco de evasão: tudo que precisa vir junto, numa consulta. */
    @Query("""
            select m from Matricula m
            join fetch m.aluno a join fetch a.unidade join fetch m.plano
            where m.status = 'ATIVA'
            """)
    List<Matricula> ativasComAlunoEPlano();

    /** Receita recorrente mensal: soma dos planos das matrículas ativas. */
    @Query("select coalesce(sum(p.valorMensal), 0) from Matricula m join m.plano p where m.status = 'ATIVA'")
    BigDecimal receitaRecorrente();

    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$PorUnidade(a.unidade.id, count(m), coalesce(sum(p.valorMensal), 0))
            from Matricula m join m.aluno a join m.plano p
            where m.status = 'ATIVA' group by a.unidade.id
            """)
    List<PorUnidade> ativasPorUnidade();

    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$PorPlano(p.nome, count(m), coalesce(sum(p.valorMensal), 0))
            from Matricula m join m.plano p
            where m.status = 'ATIVA' group by p.nome order by count(m) desc
            """)
    List<PorPlano> ativasPorPlano();

    /** Receita recorrente de uma data passada (matrículas vigentes naquele dia), para a tendência do painel. */
    @Query("""
            select coalesce(sum(p.valorMensal), 0) from Matricula m join m.plano p
            where m.inicio <= :data and (m.fim is null or m.fim > :data) and m.status <> 'TRANCADA'
              and (:unidadeId is null or m.aluno.unidade.id = :unidadeId)
            """)
    BigDecimal receitaEm(@Param("data") LocalDate data, @Param("unidadeId") Long unidadeId);

    @Query("""
            select count(m) from Matricula m
            where m.inicio <= :data and (m.fim is null or m.fim > :data) and m.status <> 'TRANCADA'
              and (:unidadeId is null or m.aluno.unidade.id = :unidadeId)
            """)
    long ativasEm(@Param("data") LocalDate data, @Param("unidadeId") Long unidadeId);

    long countByInicioGreaterThanEqual(LocalDate desde);

    long countByStatusAndFimGreaterThanEqual(StatusMatricula status, LocalDate desde);

    /** JPQL navegando pela associação (a consulta do desafio original): matrículas por bairro do aluno. */
    @Query("""
            select m from Matricula m join fetch m.aluno a join fetch m.plano
            where lower(a.endereco.bairro) = lower(:bairro)
            order by a.nome
            """)
    List<Matricula> buscarPorBairroDoAluno(@Param("bairro") String bairro);

    // --- Laboratório: quatro jeitos de listar a mesma coisa, com custos de SQL bem diferentes.

    /** O padrão: nada é buscado junto. Tocar em aluno/unidade/plano dispara uma consulta por linha (N+1). */
    Page<Matricula> findByStatus(StatusMatricula status, Pageable pagina);

    @EntityGraph(attributePaths = { "aluno", "aluno.unidade", "plano" })
    Page<Matricula> findComGrafoByStatus(StatusMatricula status, Pageable pagina);

    @Query(value = """
            select m from Matricula m join fetch m.aluno a join fetch a.unidade join fetch m.plano
            where m.status = :status order by m.id
            """, countQuery = "select count(m) from Matricula m where m.status = :status")
    Page<Matricula> findComJoinFetch(@Param("status") StatusMatricula status, Pageable pagina);

    @Query(value = """
            select new dev.barboza.pulso.repositorio.Resumos$LinhaDaMatricula(m.id, a.nome, u.nome, p.nome)
            from Matricula m join m.aluno a join a.unidade u join m.plano p
            where m.status = :status order by m.id
            """, countQuery = "select count(m) from Matricula m where m.status = :status")
    Page<LinhaDaMatricula> projetar(@Param("status") StatusMatricula status, Pageable pagina);
}
