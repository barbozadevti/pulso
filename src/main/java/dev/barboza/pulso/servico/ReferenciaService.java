package dev.barboza.pulso.servico;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.repositorio.ModalidadeRepository;
import dev.barboza.pulso.repositorio.PlanoRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;

/** Dados de referência para os seletores das telas: unidades, planos (com modalidades) e modalidades. */
@Service
@Transactional(readOnly = true)
public class ReferenciaService {

    public record UnidadeRef(Long id, String nome, String cidade, String uf, int capacidade) {
    }

    public record ModalidadeRef(Long id, String nome, String icone) {
    }

    public record PlanoRef(Long id, String nome, BigDecimal valorMensal, int duracaoMeses, boolean multiUnidade,
            List<ModalidadeRef> modalidades) {
    }

    private final UnidadeRepository unidades;
    private final PlanoRepository planos;
    private final ModalidadeRepository modalidades;

    public ReferenciaService(UnidadeRepository unidades, PlanoRepository planos, ModalidadeRepository modalidades) {
        this.unidades = unidades;
        this.planos = planos;
        this.modalidades = modalidades;
    }

    public List<UnidadeRef> unidades() {
        return unidades.findAllByOrderByNome().stream()
                .map(u -> new UnidadeRef(u.getId(), u.getNome(), u.getCidade(), u.getUf(), u.getCapacidade())).toList();
    }

    public List<ModalidadeRef> modalidades() {
        return modalidades.findAll().stream().map(m -> new ModalidadeRef(m.getId(), m.getNome(), m.getIcone()))
                .toList();
    }

    public List<PlanoRef> planos() {
        return planos.findAll().stream()
                .sorted(java.util.Comparator.comparing(p -> p.getValorMensal()))
                .map(p -> new PlanoRef(p.getId(), p.getNome(), p.getValorMensal(), p.getDuracaoMeses(),
                        p.isMultiUnidade(), p.getModalidades().stream()
                                .map(m -> new ModalidadeRef(m.getId(), m.getNome(), m.getIcone())).toList()))
                .toList();
    }
}
