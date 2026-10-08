package dev.barboza.pulso.api;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.api.Dtos.AlunoEntrada;
import dev.barboza.pulso.api.Dtos.AvaliacaoEntrada;
import dev.barboza.pulso.api.Dtos.Pagina;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.servico.AlunoConsultaService;
import dev.barboza.pulso.servico.AlunoService;
import dev.barboza.pulso.servico.AlunoService.DadosDoAluno;
import dev.barboza.pulso.servico.Visoes.AvaliacaoVisao;
import dev.barboza.pulso.servico.Visoes.Ficha;
import dev.barboza.pulso.servico.Visoes.ResumoDoAluno;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Alunos")
public class AlunoController {

    private final AlunoService alunos;
    private final AlunoConsultaService consulta;

    public AlunoController(AlunoService alunos, AlunoConsultaService consulta) {
        this.alunos = alunos;
        this.consulta = consulta;
    }

    @GetMapping("/alunos")
    @Operation(summary = "Lista alunos com filtros combináveis (Specification) e paginação")
    Pagina<ResumoDoAluno> listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) Long unidadeId, @RequestParam(required = false) String bairro,
            @RequestParam(required = false) StatusMatricula situacao,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        var pag = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanho), 100), Sort.by("nome"));
        return Pagina.de(consulta.listar(busca, unidadeId, bairro, situacao, pag));
    }

    @GetMapping("/alunos/{id}")
    @Operation(summary = "Ficha completa: matrículas, cobranças, avaliações, frequência, risco e reservas")
    Ficha ficha(@PathVariable Long id) {
        return consulta.ficha(id);
    }

    @PostMapping("/alunos")
    ResponseEntity<Ficha> criar(@Valid @RequestBody AlunoEntrada e) {
        var criado = alunos.criar(dados(e));
        return ResponseEntity.created(URI.create("/api/alunos/" + criado.getId())).body(consulta.ficha(criado.getId()));
    }

    @PutMapping("/alunos/{id}")
    Ficha atualizar(@PathVariable Long id, @Valid @RequestBody AlunoEntrada e) {
        alunos.atualizar(id, dados(e));
        return consulta.ficha(id);
    }

    @GetMapping("/alunos/{id}/avaliacoes")
    List<AvaliacaoVisao> avaliacoes(@PathVariable Long id) {
        return consulta.ficha(id).avaliacoes();
    }

    @PostMapping("/alunos/{id}/avaliacoes")
    ResponseEntity<List<AvaliacaoVisao>> registrar(@PathVariable Long id, @Valid @RequestBody AvaliacaoEntrada e) {
        var a = alunos.registrarAvaliacao(id, e.data(), e.peso(), e.altura(), e.percentualGordura(), e.cinturaCm());
        return ResponseEntity.created(URI.create("/api/avaliacoes/" + a.getId())).body(consulta.ficha(id).avaliacoes());
    }

    @PutMapping("/avaliacoes/{id}")
    void atualizarAvaliacao(@PathVariable Long id, @Valid @RequestBody AvaliacaoEntrada e) {
        alunos.atualizarAvaliacao(id, e.data(), e.peso(), e.altura(), e.percentualGordura(), e.cinturaCm());
    }

    @DeleteMapping("/avaliacoes/{id}")
    ResponseEntity<Void> removerAvaliacao(@PathVariable Long id) {
        alunos.removerAvaliacao(id);
        return ResponseEntity.noContent().build();
    }

    private static DadosDoAluno dados(AlunoEntrada e) {
        return new DadosDoAluno(e.nome(), e.cpf(), e.email(), e.nascimento(), e.bairro(), e.cidade(), e.uf(),
                e.unidadeId());
    }
}
