package dev.barboza.pulso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.barboza.pulso.dominio.Plano;

public interface PlanoRepository extends JpaRepository<Plano, Long> {

    /** EntityGraph: traz as modalidades junto, numa consulta só (o padrão seria uma por plano). */
    @Override
    @EntityGraph(attributePaths = "modalidades")
    List<Plano> findAll();
}
