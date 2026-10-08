package dev.barboza.pulso.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.rastro.HistoricoSql;
import dev.barboza.pulso.rastro.HistoricoSql.Registro;
import dev.barboza.pulso.servico.LaboratorioService;
import dev.barboza.pulso.servico.LaboratorioService.Comparacao;
import dev.barboza.pulso.servico.LaboratorioService.Disputa;
import dev.barboza.pulso.servico.LaboratorioService.Modo;
import dev.barboza.pulso.servico.PainelService;
import dev.barboza.pulso.servico.PainelService.Painel;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Nivel;
import dev.barboza.pulso.servico.ReferenciaService;
import dev.barboza.pulso.servico.ReferenciaService.ModalidadeRef;
import dev.barboza.pulso.servico.ReferenciaService.PlanoRef;
import dev.barboza.pulso.servico.ReferenciaService.UnidadeRef;
import dev.barboza.pulso.servico.RiscoService;
import dev.barboza.pulso.servico.RiscoService.AlunoEmRisco;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Painel executivo, risco de evasão, referências e laboratório de JPA. */
@RestController
@RequestMapping("/api")
@Tag(name = "Gestão")
public class GestaoController {

    private final PainelService painel;
    private final RiscoService risco;
    private final ReferenciaService referencia;
    private final LaboratorioService laboratorio;
    private final HistoricoSql historico;

    public GestaoController(PainelService painel, RiscoService risco, ReferenciaService referencia,
            LaboratorioService laboratorio, HistoricoSql historico) {
        this.painel = painel;
        this.risco = risco;
        this.referencia = referencia;
        this.laboratorio = laboratorio;
        this.historico = historico;
    }

    @GetMapping("/painel")
    @Operation(summary = "Painel executivo da rede (ou de uma unidade): receita, churn, inadimplência, ocupação, risco")
    Painel painel(@RequestParam(required = false) Long unidadeId) {
        return painel.painel(unidadeId);
    }

    @GetMapping("/risco")
    @Operation(summary = "Alunos com risco de evasão, do maior para o menor, com os motivos")
    List<AlunoEmRisco> risco(@RequestParam(required = false) Long unidadeId,
            @RequestParam(required = false) Nivel nivel) {
        return risco.ranking(unidadeId).stream().filter(r -> nivel == null || r.nivel() == nivel).toList();
    }

    @GetMapping(value = "/risco.csv", produces = "text/csv")
    @Operation(summary = "Lista de risco em CSV (Excel em português: separador ponto e vírgula, com BOM)")
    org.springframework.http.ResponseEntity<byte[]> riscoCsv(@RequestParam(required = false) Long unidadeId,
            @RequestParam(required = false) Nivel nivel) {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"risco-de-evasao.csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(RiscoCsv.gerar(risco.ranking(unidadeId).stream()
                        .filter(r -> nivel == null || r.nivel() == nivel).toList()));
    }

    @GetMapping("/unidades")
    List<UnidadeRef> unidades() {
        return referencia.unidades();
    }

    @GetMapping("/planos")
    List<PlanoRef> planos() {
        return referencia.planos();
    }

    @GetMapping("/modalidades")
    List<ModalidadeRef> modalidades() {
        return referencia.modalidades();
    }

    // ---- laboratório

    @GetMapping("/laboratorio/n-mais-um")
    @Operation(summary = "Mede o SQL real de quatro jeitos de carregar as mesmas 50 matrículas")
    List<Comparacao> nMaisUm() {
        return java.util.Arrays.stream(Modo.values()).map(laboratorio::comparar).toList();
    }

    @PostMapping("/laboratorio/disputa")
    @Operation(summary = "N threads disputam as vagas de uma aula ao mesmo tempo; confere que não há overbooking")
    Disputa disputa(@RequestParam(defaultValue = "24") int concorrentes, @RequestParam(defaultValue = "3") int vagas) {
        return laboratorio.disputar(concorrentes, vagas);
    }

    @GetMapping("/rastro/recentes")
    @Operation(summary = "Últimas requisições da API com o SQL que cada uma gerou")
    List<Registro> recentes(@RequestParam(defaultValue = "30") int quantos) {
        return historico.recentes(Math.min(quantos, 60));
    }
}
