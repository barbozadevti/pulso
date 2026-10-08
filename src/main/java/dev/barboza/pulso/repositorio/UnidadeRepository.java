package dev.barboza.pulso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.barboza.pulso.dominio.Unidade;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findAllByOrderByNome();
}
