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
import dev.barboza.pulso.seguranca.Escopo;
import dev.barboza.pulso.servico.AlunoConsultaService;
import dev.barboza.pulso.servico.AlunoService;
import dev.barboza.pulso.servico.AlunoService.DadosDoAluno;
import dev.barboza.pulso.servico.ContatoService;
import dev.barboza.pulso.api.Dtos.ContatoEntrada;
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
    private final ContatoService contatos;
    private final Escopo escopo;

    public AlunoController(AlunoService alunos, AlunoConsultaService consulta, ContatoService contatos,
            Escopo escopo) {
        this.escopo = escopo;
        this.alunos = alunos;
        this.consulta = consulta;
        this.contatos = contatos;
    }

    @GetMapping("/alunos")
    @Operation(summary = "Lista alunos com filtros combináveis (Specification) e paginação")
    Pagina<ResumoDoAluno> listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) Long unidadeId, @RequestParam(required = false) String bairro,
            @RequestParam(required = false) StatusMatricula situacao,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        var pag = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanho), 100), Sort.by("nome"));
        return Pagina.de(consulta.listar(busca, escopo.unidade(unidadeId), bairro, situacao, pag));
    }

    @GetMapping("/alunos/{id}")
    @Operation(summary = "Ficha completa: matrículas, cobranças, avaliações, frequência, risco e reservas")
    Ficha ficha(@PathVariable Long id) {
        escopo.aluno(id);
        return escopo.paraOPerfil(consulta.ficha(id));
    }

    @PostMapping("/alunos")
    ResponseEntity<Ficha> criar(@Valid @RequestBody AlunoEntrada e) {
        escopo.unidade(e.unidadeId()); // quem é de uma unidade só cadastra nela
        var criado = alunos.criar(dados(e));
        return ResponseEntity.created(URI.create("/api/alunos/" + criado.getId()))
                .body(escopo.paraOPerfil(consulta.ficha(criado.getId())));
    }

    @PutMapping("/alunos/{id}")
    Ficha atualizar(@PathVariable Long id, @Valid @RequestBody AlunoEntrada e) {
        escopo.aluno(id);
        alunos.atualizar(id, dados(e));
        return escopo.paraOPerfil(consulta.ficha(id));
    }

    @GetMapping("/alunos/{id}/avaliacoes")
    List<AvaliacaoVisao> avaliacoes(@PathVariable Long id) {
        escopo.aluno(id);
        return consulta.ficha(id).avaliacoes();
    }

    @PostMapping("/alunos/{id}/avaliacoes")
    ResponseEntity<List<AvaliacaoVisao>> registrar(@PathVariable Long id, @Valid @RequestBody AvaliacaoEntrada e) {
        escopo.aluno(id);
        var a = alunos.registrarAvaliacao(id, e.data(), e.peso(), e.altura(), e.percentualGordura(), e.cinturaCm());
        return ResponseEntity.created(URI.create("/api/avaliacoes/" + a.getId())).body(consulta.ficha(id).avaliacoes());
    }

    @PostMapping("/alunos/{id}/contatos")
    @Operation(summary = "Registra uma abordagem de retenção (ligação, WhatsApp ou presencial) e o resultado")
    ResponseEntity<Ficha> registrarContato(@PathVariable Long id, @Valid @RequestBody ContatoEntrada e) {
        escopo.aluno(id);
        contatos.registrar(id, e.canal(), e.resultado(), e.observacao());
        return ResponseEntity.status(201).body(escopo.paraOPerfil(consulta.ficha(id)));
    }

    @PutMapping("/avaliacoes/{id}")
    void atualizarAvaliacao(@PathVariable Long id, @Valid @RequestBody AvaliacaoEntrada e) {
        escopo.avaliacao(id);
        alunos.atualizarAvaliacao(id, e.data(), e.peso(), e.altura(), e.percentualGordura(), e.cinturaCm());
    }

    @DeleteMapping("/avaliacoes/{id}")
    ResponseEntity<Void> removerAvaliacao(@PathVariable Long id) {
        escopo.avaliacao(id);
        alunos.removerAvaliacao(id);
        return ResponseEntity.noContent().build();
    }

    private static DadosDoAluno dados(AlunoEntrada e) {
        return new DadosDoAluno(e.nome(), e.cpf(), e.email(), e.nascimento(), e.bairro(), e.cidade(), e.uf(),
                e.unidadeId());
    }
}
