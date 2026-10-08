package dev.barboza.pulso.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Reserva;
import dev.barboza.pulso.dominio.StatusReserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Contagem por aula e status numa consulta só (evita carregar a coleção de cada aula). */
    @Query("select r.aula.id, r.status, count(r) from Reserva r where r.aula.id in :aulas and r.status <> 'CANCELADA' group by r.aula.id, r.status")
    List<Object[]> contagemPorAula(@Param("aulas") Collection<Long> aulas);

    @Query("select r.aula.id from Reserva r where r.aluno.id = :alunoId and r.aula.id in :aulas and r.status <> 'CANCELADA'")
    List<Long> aulasDoAluno(@Param("alunoId") Long alunoId, @Param("aulas") Collection<Long> aulas);

    @Query("""
            select r from Reserva r join fetch r.aula a join fetch a.modalidade join fetch a.unidade
            where r.aluno.id = :alunoId and r.status <> 'CANCELADA' and a.inicio >= :agora order by a.inicio
            """)
    List<Reserva> futurasDoAluno(@Param("alunoId") Long alunoId, @Param("agora") LocalDateTime agora);

    long countByAulaIdAndStatus(Long aulaId, StatusReserva status);
}
