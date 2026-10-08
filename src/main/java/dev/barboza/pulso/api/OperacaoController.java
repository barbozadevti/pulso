package dev.barboza.pulso.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.api.Dtos.EntradaNaCatraca;
import dev.barboza.pulso.api.Dtos.ReservaEntrada;
import dev.barboza.pulso.api.Dtos.SaidaDaCatraca;
import dev.barboza.pulso.servico.CatracaService;
import dev.barboza.pulso.servico.CatracaService.AoVivo;
import dev.barboza.pulso.servico.CatracaService.Decisao;
import dev.barboza.pulso.servico.Erros;
import dev.barboza.pulso.servico.ReservaService;
import dev.barboza.pulso.servico.ReservaService.AulaVisao;
import dev.barboza.pulso.servico.ReservaService.Cancelamento;
import dev.barboza.pulso.servico.ReservaService.ResultadoDaReserva;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** O dia a dia da unidade: catraca e aulas com reserva. */
@RestController
@RequestMapping("/api")
@Tag(name = "Catraca e aulas")
public class OperacaoController {

    private final CatracaService catraca;
    private final ReservaService reservas;
    private final Clock relogio;

    public OperacaoController(CatracaService catraca, ReservaService reservas, Clock relogio) {
        this.catraca = catraca;
        this.reservas = reservas;
        this.relogio = relogio;
    }

    @PostMapping("/catraca/entrada")
    @Operation(summary = "Tenta liberar a catraca. Sempre responde 200 com liberado=true/false e o motivo.")
    Decisao entrar(@Valid @RequestBody EntradaNaCatraca e) {
        if (e.alunoId() != null) {
            return catraca.entrarPorId(e.alunoId(), e.unidadeId());
        }
        if (e.cpf() == null || e.cpf().isBlank()) {
            throw new Erros.RegraDeNegocio("Informe o CPF ou o aluno.");
        }
        return catraca.entrar(e.cpf(), e.unidadeId());
    }

    @PostMapping("/catraca/saida")
    Decisao sair(@Valid @RequestBody SaidaDaCatraca e) {
        return catraca.sair(e.alunoId());
    }

    @GetMapping("/catraca/ao-vivo")
    AoVivo aoVivo(@RequestParam Long unidadeId) {
        return catraca.aoVivo(unidadeId);
    }

    @GetMapping("/aulas")
    @Operation(summary = "Agenda do dia com vagas, fila de espera e se o aluno já reservou")
    List<AulaVisao> agenda(@RequestParam(required = false) Long unidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Long alunoId) {
        return reservas.agenda(unidadeId, dia == null ? LocalDate.now(relogio) : dia, alunoId);
    }

    @PostMapping("/aulas/{id}/reservas")
    @Operation(summary = "Reserva a vaga; se a aula estiver cheia, entra na fila de espera (bloqueio otimista)")
    ResponseEntity<ResultadoDaReserva> reservar(@PathVariable Long id, @Valid @RequestBody ReservaEntrada e) {
        return ResponseEntity.status(201).body(reservas.reservar(id, e.alunoId()));
    }

    @DeleteMapping("/reservas/{id}")
    @Operation(summary = "Cancela a reserva e promove o primeiro da fila de espera")
    Cancelamento cancelar(@PathVariable Long id) {
        return reservas.cancelar(id);
    }
}
