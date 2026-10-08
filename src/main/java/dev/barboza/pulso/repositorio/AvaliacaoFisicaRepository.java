package dev.barboza.pulso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.barboza.pulso.dominio.AvaliacaoFisica;

public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisica, Long> {

    List<AvaliacaoFisica> findByAlunoIdOrderByDataAsc(Long alunoId);
}
