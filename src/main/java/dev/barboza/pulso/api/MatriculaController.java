package dev.barboza.pulso.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.api.Dtos.CancelamentoEntrada;
import dev.barboza.pulso.api.Dtos.MatriculaEntrada;
import dev.barboza.pulso.api.Dtos.PagamentoEntrada;
import dev.barboza.pulso.api.Dtos.TrocaDePlanoEntrada;
import dev.barboza.pulso.seguranca.Escopo;
import dev.barboza.pulso.servico.MatriculaService;
import dev.barboza.pulso.servico.MatriculaService.Atrasada;
import dev.barboza.pulso.servico.MatriculaService.LinhaDaMatricula;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Matrículas e cobrança")
public class MatriculaController {

    private final MatriculaService servico;
    private final Escopo escopo;

    public MatriculaController(MatriculaService servico, Escopo escopo) {
        this.servico = servico;
        this.escopo = escopo;
    }

    @GetMapping("/matriculas")
    @Operation(summary = "Matrículas por bairro do aluno (JPQL com join fetch)")
    List<LinhaDaMatricula> porBairro(@RequestParam(required = false) String bairro) {
        return servico.porBairro(bairro, escopo.unidade(null));
    }

    public record Resposta(Long id, String status) {
    }

    @PostMapping("/matriculas")
    ResponseEntity<Resposta> matricular(@Valid @RequestBody MatriculaEntrada e) {
        escopo.aluno(e.alunoId());
        var m = servico.matricular(e.alunoId(), e.planoId());
        return ResponseEntity.created(URI.create("/api/alunos/" + e.alunoId()))
                .body(new Resposta(m.getId(), m.getStatus().name()));
    }

    @PostMapping("/matriculas/{id}/cancelar")
    Resposta cancelar(@PathVariable Long id, @Valid @RequestBody(required = false) CancelamentoEntrada e) {
        escopo.matricula(id);
        var m = servico.cancelar(id, e == null ? null : e.motivo());
        return new Resposta(m.getId(), m.getStatus().name());
    }

    @PostMapping("/matriculas/{id}/trancar")
    Resposta trancar(@PathVariable Long id) {
        escopo.matricula(id);
        var m = servico.trancar(id);
        return new Resposta(m.getId(), m.getStatus().name());
    }

    @PostMapping("/matriculas/{id}/reativar")
    Resposta reativar(@PathVariable Long id) {
        escopo.matricula(id);
        var m = servico.reativar(id);
        return new Resposta(m.getId(), m.getStatus().name());
    }

    @PostMapping("/matriculas/{id}/plano")
    Resposta mudarPlano(@PathVariable Long id, @Valid @RequestBody TrocaDePlanoEntrada e) {
        escopo.matricula(id);
        var m = servico.mudarPlano(id, e.planoId());
        return new Resposta(m.getId(), m.getStatus().name());
    }

    @GetMapping("/mensalidades/atrasadas")
    List<Atrasada> atrasadas() {
        return servico.atrasadas(escopo.unidade(null));
    }

    @PostMapping("/mensalidades/{id}/pagar")
    Resposta pagar(@PathVariable Long id, @Valid @RequestBody PagamentoEntrada e) {
        escopo.mensalidade(id);
        var m = servico.pagar(id, e.forma(), e.referencia());
        return new Resposta(m.getId(), "PAGA");
    }
}
