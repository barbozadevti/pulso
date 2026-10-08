package dev.barboza.pulso.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Contato;
import dev.barboza.pulso.repositorio.Resumos.UltimoContato;

public interface ContatoRepository extends JpaRepository<Contato, Long> {

    List<Contato> findByAlunoIdOrderByFeitoEmDesc(Long alunoId);

    /** O contato mais recente de cada aluno, numa consulta só (subconsulta correlacionada). */
    @Query("""
            select new dev.barboza.pulso.repositorio.Resumos$UltimoContato(c.aluno.id, c.feitoEm, c.resultado)
            from Contato c
            where c.feitoEm = (select max(c2.feitoEm) from Contato c2 where c2.aluno.id = c.aluno.id)
            """)
    List<UltimoContato> ultimoDeCadaAluno();

    @Query("""
            select count(c) from Contato c
            where c.feitoEm >= :desde and (:unidadeId is null or c.aluno.unidade.id = :unidadeId)
            """)
    long contatosDesde(@Param("desde") LocalDateTime desde, @Param("unidadeId") Long unidadeId);

    /** Quem voltou a treinar depois de ser abordado (sem repetir o aluno que foi contatado várias vezes). */
    @Query("""
            select distinct c.aluno.id from Contato c
            where c.resultado = 'VOLTOU_A_TREINAR' and c.feitoEm >= :desde
              and (:unidadeId is null or c.aluno.unidade.id = :unidadeId)
            """)
    List<Long> alunosQueVoltaram(@Param("desde") LocalDateTime desde, @Param("unidadeId") Long unidadeId);
}
