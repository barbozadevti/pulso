package dev.barboza.pulso.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.barboza.pulso.dominio.Instrutor;

public interface InstrutorRepository extends JpaRepository<Instrutor, Long> {

    List<Instrutor> findByUnidadeId(Long unidadeId);
}
