package dev.barboza.pulso.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.barboza.pulso.dominio.Acesso;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;
import dev.barboza.pulso.servico.CatracaService;
import dev.barboza.pulso.servico.Erros;

/**
 * Faz a catraca "viver" na demonstração: a cada poucos segundos alguém entra ou sai, no ritmo do horário
 * (pico de manhã cedo e fim de tarde). Desligue com {@code pulso.simulador=false}.
 */
@Component
@ConditionalOnProperty(name = "pulso.simulador", havingValue = "true")
public class SimuladorDeMovimento {

    private final Random sorteio = new Random();
    private final CatracaService catraca;
    private final AcessoRepository acessos;
    private final MatriculaRepository matriculas;
    private final UnidadeRepository unidades;
    private final Clock relogio;
    private List<Long> alunosAtivos = List.of();
    private LocalDateTime carregadoEm = LocalDateTime.MIN;

    public SimuladorDeMovimento(CatracaService catraca, AcessoRepository acessos, MatriculaRepository matriculas,
            UnidadeRepository unidades, Clock relogio) {
        this.catraca = catraca;
        this.acessos = acessos;
        this.matriculas = matriculas;
        this.unidades = unidades;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelay = 3500, initialDelay = 8000)
    public void passo() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        int hora = agora.getHour();
        if (hora < 6 || hora >= 22) {
            fecharTudo(agora);
            return;
        }
        // Mais gente chegando nos picos; fora deles, mais gente saindo.
        double pico = (hora >= 6 && hora <= 8) || (hora >= 17 && hora <= 20) ? 0.65 : 0.35;
        if (sorteio.nextDouble() < pico) {
            entrarAlguem();
        } else {
            sairAlguem(agora);
        }
    }

    private void entrarAlguem() {
        recarregarSeNecessario();
        var unids = unidades.findAll();
        if (alunosAtivos.isEmpty() || unids.isEmpty()) {
            return;
        }
        Long aluno = alunosAtivos.get(sorteio.nextInt(alunosAtivos.size()));
        Long unidade = unids.get(sorteio.nextInt(unids.size())).getId();
        try {
            catraca.entrarPorId(aluno, unidade);
        } catch (Erros.NaoEncontrado | Erros.RegraDeNegocio ignorado) {
            // simulação: se a catraca negar, tudo bem
        }
    }

    private void sairAlguem(LocalDateTime agora) {
        List<Acesso> dentro = acessos.findBySaidaIsNull();
        if (dentro.isEmpty()) {
            return;
        }
        // Prefere quem já está há mais de 40 minutos.
        List<Acesso> candidatos = dentro.stream().filter(a -> a.getEntrada().isBefore(agora.minusMinutes(40))).toList();
        List<Acesso> base = candidatos.isEmpty() ? dentro : candidatos;
        Acesso escolhido = base.get(sorteio.nextInt(base.size()));
        escolhido.sair(agora);
        acessos.save(escolhido);
    }

    private void fecharTudo(LocalDateTime agora) {
        for (Acesso a : acessos.findBySaidaIsNull()) {
            a.sair(agora);
            acessos.save(a);
        }
    }

    private void recarregarSeNecessario() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (carregadoEm.isBefore(agora.minusHours(1))) {
            List<Long> ids = new ArrayList<>();
            matriculas.ativasComAlunoEPlano().forEach(m -> ids.add(m.getAluno().getId()));
            alunosAtivos = ids;
            carregadoEm = agora;
        }
    }
}
