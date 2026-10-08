package dev.barboza.pulso.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Aula;
import jakarta.persistence.LockModeType;

public interface AulaRepository extends JpaRepository<Aula, Long> {

    @Query("""
            select a from Aula a
            join fetch a.modalidade join fetch a.instrutor join fetch a.unidade
            where (:unidadeId is null or a.unidade.id = :unidadeId)
              and a.inicio >= :de and a.inicio < :ate
            order by a.inicio
            """)
    List<Aula> daAgenda(@Param("unidadeId") Long unidadeId, @Param("de") LocalDateTime de,
            @Param("ate") LocalDateTime ate);

    /**
     * Busca a aula forçando o incremento de @Version no commit. Duas transações que reservam a mesma aula
     * ao mesmo tempo disputam essa linha: a que chega depois leva OptimisticLockException e refaz.
     * Sem join fetch de propósito: o bloqueio vale para toda entidade trazida pela consulta, e só a Aula tem @Version.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select a from Aula a where a.id = :id")
    Optional<Aula> buscarParaReservar(@Param("id") Long id);

    @Query("select a from Aula a join fetch a.unidade join fetch a.modalidade join fetch a.instrutor where a.id = :id")
    Optional<Aula> buscarCompleta(@Param("id") Long id);
}
