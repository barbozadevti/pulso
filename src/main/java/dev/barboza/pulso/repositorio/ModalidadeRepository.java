package dev.barboza.pulso.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.barboza.pulso.dominio.Modalidade;

public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {
}
