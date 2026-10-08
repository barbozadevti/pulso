package dev.barboza.pulso.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Unidade;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.ContatoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.MensalidadeRepository;
import dev.barboza.pulso.repositorio.Resumos.PorMes;
import dev.barboza.pulso.repositorio.Resumos.PorPlano;
import dev.barboza.pulso.repositorio.Resumos.PorUnidade;
import dev.barboza.pulso.repositorio.UnidadeRepository;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Nivel;
import dev.barboza.pulso.servico.RiscoService.AlunoEmRisco;

/** Painel executivo da rede: cada número vem de uma consulta agregada no banco (não de listas em memória). */
@Service
@Transactional(readOnly = true)
public class PainelService {

    public record Kpis(long ativos, long trancadas, BigDecimal receitaRecorrente, long novos30Dias,
            long cancelamentos30Dias, double churnPercentual, long inadimplentes, BigDecimal valorEmAtraso,
            double inadimplenciaPercentual, long visitasHoje, long dentroAgora, long capacidadeTotal,
            double variacaoReceitaPercentual, long ativosHa30Dias, long contatos7Dias, long alunosRecuperados,
            BigDecimal receitaRecuperada) {
    }

    public record UnidadeVisao(Long id, String nome, String cidade, String uf, long ativos, BigDecimal receita,
            long dentro, int capacidade, long riscoAlto) {
    }

    public record PlanoVisao(String plano, long alunos, BigDecimal receita) {
    }

    public record MesVisao(String mes, BigDecimal faturado, BigDecimal recebido) {
    }

    /** Entradas dos últimos 28 dias: linhas = segunda a domingo, colunas = horas 0 a 23. */
    public record Mapa(List<List<Integer>> valores, int maximo) {
    }

    public record Painel(LocalDateTime geradoEm, Kpis kpis, List<UnidadeVisao> unidades, List<PlanoVisao> planos,
            List<MesVisao> receita, Mapa mapaDeCalor, List<AlunoEmRisco> maisArriscados,
            BigDecimal receitaEmRisco, long alunosEmRiscoAlto, int conflitosResolvidos) {
    }

    private final MatriculaRepository matriculas;
    private final MensalidadeRepository mensalidades;
    private final AcessoRepository acessos;
    private final UnidadeRepository unidades;
    private final ContatoRepository contatos;
    private final RiscoService risco;
    private final ReservaService reservas;
    private final Clock relogio;

    public PainelService(MatriculaRepository matriculas, MensalidadeRepository mensalidades, AcessoRepository acessos,
            UnidadeRepository unidades, ContatoRepository contatos, RiscoService risco, ReservaService reservas,
            Clock relogio) {
        this.matriculas = matriculas;
        this.mensalidades = mensalidades;
        this.acessos = acessos;
        this.unidades = unidades;
        this.contatos = contatos;
        this.risco = risco;
        this.reservas = reservas;
        this.relogio = relogio;
    }

    public Painel painel(Long unidadeId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        List<Unidade> todas = unidades.findAllByOrderByNome();
        Map<Long, PorUnidade> ativas = indexar(matriculas.ativasPorUnidade());
        Map<Long, Long> dentro = new HashMap<>();
        acessos.dentroPorUnidade().forEach(c -> dentro.put(c.unidadeId(), c.quantidade()));
        List<AlunoEmRisco> ranking = risco.ranking(null);

        List<UnidadeVisao> visoes = new ArrayList<>();
        for (Unidade u : todas) {
            PorUnidade a = ativas.get(u.getId());
            Long d = dentro.get(u.getId());
            long alto = ranking.stream()
                    .filter(r -> r.unidadeId().equals(u.getId()) && r.nivel() == Nivel.ALTO).count();
            visoes.add(new UnidadeVisao(u.getId(), u.getNome(), u.getCidade(), u.getUf(),
                    a == null ? 0 : a.quantidade(), a == null ? BigDecimal.ZERO : a.valor(),
                    d == null ? 0 : d, u.getCapacidade(), alto));
        }

        List<UnidadeVisao> escopo = unidadeId == null ? visoes
                : visoes.stream().filter(v -> v.id().equals(unidadeId)).toList();
        List<AlunoEmRisco> rankingEscopo = unidadeId == null ? ranking
                : ranking.stream().filter(r -> r.unidadeId().equals(unidadeId)).toList();

        long ativosRede = escopo.stream().mapToLong(UnidadeVisao::ativos).sum();
        BigDecimal mrr = escopo.stream().map(UnidadeVisao::receita).reduce(BigDecimal.ZERO, BigDecimal::add);
        long cancelados = matriculas.canceladasDesde(hoje.minusDays(30), unidadeId);
        BigDecimal atraso = mensalidades.valorEmAtraso(hoje, unidadeId);
        long dentroAgora = escopo.stream().mapToLong(UnidadeVisao::dentro).sum();
        long capacidade = escopo.stream().mapToLong(UnidadeVisao::capacidade).sum();

        List<Long> voltaram = contatos.alunosQueVoltaram(agora.minusDays(30), unidadeId);
        List<Matricula> recuperados = voltaram.isEmpty() ? List.of()
                : matriculas.vigentesDosAlunos(voltaram).stream().filter(Matricula::ativa).toList();
        BigDecimal receitaRecuperada = recuperados.stream().map(m -> m.getPlano().getValorMensal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Kpis kpis = new Kpis(ativosRede, matriculas.trancadas(unidadeId), mrr,
                matriculas.novosDesde(hoje.minusDays(30), unidadeId), cancelados,
                percentual(cancelados, ativosRede + cancelados), mensalidades.alunosInadimplentes(hoje, unidadeId), atraso,
                mrr.signum() == 0 ? 0 : atraso.multiply(BigDecimal.valueOf(100)).divide(mrr, 1, RoundingMode.HALF_UP)
                        .doubleValue(),
                acessos.contarEntradasDesde(hoje.atStartOfDay(), unidadeId), dentroAgora, capacidade, variacao(unidadeId, hoje),
                matriculas.ativasEm(hoje.minusDays(30), unidadeId), contatos.contatosDesde(agora.minusDays(7), unidadeId),
                recuperados.size(), receitaRecuperada);

        List<AlunoEmRisco> altos = rankingEscopo.stream().filter(r -> r.nivel() == Nivel.ALTO).toList();
        BigDecimal receitaEmRisco = altos.stream().map(AlunoEmRisco::valorMensal).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        List<PlanoVisao> planos = matriculas.ativasPorPlano(unidadeId).stream()
                .map((PorPlano p) -> new PlanoVisao(p.plano(), p.quantidade(), p.valor())).toList();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<MesVisao> receita = mensalidades.faturamentoPorMes(hoje.withDayOfMonth(1).minusMonths(5), unidadeId).stream()
                .map((PorMes m) -> new MesVisao(m.competencia().format(fmt), m.faturado(),
                        m.faturado().subtract(m.pendente())))
                .toList();

        return new Painel(agora, kpis, escopo, planos, receita, mapaDeCalor(agora.minusDays(28), unidadeId),
                rankingEscopo.stream().limit(8).toList(), receitaEmRisco, altos.size(),
                reservas.conflitosResolvidos());
    }

    /** Variação da receita recorrente contra 30 dias atrás, com a mesma regra nos dois lados. */
    private double variacao(Long unidadeId, LocalDate hoje) {
        BigDecimal agora = matriculas.receitaEm(hoje, unidadeId);
        BigDecimal antes = matriculas.receitaEm(hoje.minusDays(30), unidadeId);
        return antes.signum() == 0 ? 0 : agora.subtract(antes).multiply(BigDecimal.valueOf(100))
                .divide(antes, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private Mapa mapaDeCalor(LocalDateTime desde, Long unidadeId) {
        int[][] g = new int[7][24];
        for (LocalDateTime e : acessos.entradasDesde(desde, unidadeId)) {
            g[e.getDayOfWeek().getValue() - 1][e.getHour()]++;
        }
        int max = 0;
        List<List<Integer>> linhas = new ArrayList<>();
        for (int[] dia : g) {
            List<Integer> l = new ArrayList<>();
            for (int v : dia) {
                l.add(v);
                max = Math.max(max, v);
            }
            linhas.add(l);
        }
        return new Mapa(linhas, max);
    }

    private static Map<Long, PorUnidade> indexar(List<PorUnidade> lista) {
        Map<Long, PorUnidade> m = new HashMap<>();
        lista.forEach(p -> m.put(p.unidadeId(), p));
        return m;
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0 : BigDecimal.valueOf(parte * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
