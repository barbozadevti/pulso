package dev.barboza.pulso.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.barboza.pulso.dominio.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long>, JpaSpecificationExecutor<Aluno> {

    Optional<Aluno> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    /** Ficha do aluno: a unidade vem junto; as coleções são buscadas em consultas próprias, sem produto cartesiano. */
    @Query("select a from Aluno a join fetch a.unidade where a.id = :id")
    Optional<Aluno> buscarComUnidade(@Param("id") Long id);
}
