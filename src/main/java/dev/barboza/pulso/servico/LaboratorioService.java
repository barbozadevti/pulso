package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Aula;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.rastro.RastroSql;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.AulaRepository;
import dev.barboza.pulso.repositorio.InstrutorRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.ModalidadeRepository;
import dev.barboza.pulso.repositorio.ReservaRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;
import dev.barboza.pulso.dominio.StatusReserva;

/**
 * Laboratório: duas demonstrações ao vivo do que o JPA faz por baixo.
 * (1) O problema N+1 e quatro maneiras de resolvê-lo, medidas em consultas SQL reais.
 * (2) A disputa pela última vaga: dezenas de threads reservando a mesma aula ao mesmo tempo.
 */
@Service
public class LaboratorioService {

    public static final int LINHAS = 50;

    public enum Modo { INGENUO, ENTITY_GRAPH, JOIN_FETCH, PROJECAO }

    public record Comparacao(String modo, String titulo, String explicacao, int consultas, long milissegundos,
            int linhas, List<String> exemplos, List<String> sql) {
    }

    public record Disputa(int concorrentes, int vagas, long confirmadas, long naEspera, int semResposta,
            int conflitosResolvidos, long milissegundos, boolean overbooking) {
    }

    private final MatriculaRepository matriculas;
    private final AlunoRepository alunos;
    private final AulaRepository aulas;
    private final ReservaRepository reservas;
    private final ModalidadeRepository modalidades;
    private final UnidadeRepository unidades;
    private final InstrutorRepository instrutores;
    private final ReservaService reservaService;
    private final TransactionTemplate tx;
    private final Clock relogio;

    public LaboratorioService(MatriculaRepository matriculas, AlunoRepository alunos, AulaRepository aulas,
            ReservaRepository reservas, ModalidadeRepository modalidades, UnidadeRepository unidades,
            InstrutorRepository instrutores, ReservaService reservaService, PlatformTransactionManager gerenciador,
            Clock relogio) {
        this.matriculas = matriculas;
        this.alunos = alunos;
        this.aulas = aulas;
        this.reservas = reservas;
        this.modalidades = modalidades;
        this.unidades = unidades;
        this.instrutores = instrutores;
        this.reservaService = reservaService;
        this.tx = new TransactionTemplate(gerenciador);
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Comparacao comparar(Modo modo) {
        Pageable pagina = PageRequest.of(0, LINHAS);
        var medicao = RastroSql.medir(() -> {
            List<String> exemplos = new ArrayList<>();
            int linhas;
            switch (modo) {
                case INGENUO -> {
                    var p = matriculas.findByStatus(StatusMatricula.ATIVA, pagina);
                    linhas = p.getNumberOfElements();
                    // Tocar nas associações lazy de cada linha: cada toque é uma consulta nova (N+1).
                    p.forEach(m -> exemplos.add(m.getAluno().getNome() + " · " + m.getAluno().getUnidade().getNome()
                            + " · " + m.getPlano().getNome()));
                }
                case ENTITY_GRAPH -> {
                    var p = matriculas.findComGrafoByStatus(StatusMatricula.ATIVA, pagina);
                    linhas = p.getNumberOfElements();
                    p.forEach(m -> exemplos.add(m.getAluno().getNome() + " · " + m.getAluno().getUnidade().getNome()
                            + " · " + m.getPlano().getNome()));
                }
                case JOIN_FETCH -> {
                    var p = matriculas.findComJoinFetch(StatusMatricula.ATIVA, pagina);
                    linhas = p.getNumberOfElements();
                    p.forEach(m -> exemplos.add(m.getAluno().getNome() + " · " + m.getAluno().getUnidade().getNome()
                            + " · " + m.getPlano().getNome()));
                }
                default -> {
                    var p = matriculas.projetar(StatusMatricula.ATIVA, pagina);
                    linhas = p.getNumberOfElements();
                    p.forEach(l -> exemplos.add(l.aluno() + " · " + l.unidade() + " · " + l.plano()));
                }
            }
            return new Object[] { linhas, exemplos };
        });
        @SuppressWarnings("unchecked")
        List<String> exemplos = (List<String>) medicao.resultado()[1];
        return new Comparacao(modo.name(), titulo(modo), explicacao(modo), medicao.consultas(),
                medicao.milissegundos(), (Integer) medicao.resultado()[0], exemplos.stream().limit(3).toList(),
                medicao.sql().stream().limit(8).toList());
    }

    private static String titulo(Modo m) {
        return switch (m) {
            case INGENUO -> "Ingênuo (lazy padrão)";
            case ENTITY_GRAPH -> "@EntityGraph";
            case JOIN_FETCH -> "JPQL com join fetch";
            case PROJECAO -> "Projeção em DTO";
        };
    }

    private static String explicacao(Modo m) {
        return switch (m) {
            case INGENUO -> "Carrega 50 matrículas e depois, para cada uma, uma consulta para o aluno, outra para a "
                    + "unidade e outra para o plano. É o clássico N+1: o código parece inocente e o banco sofre.";
            case ENTITY_GRAPH -> "Declara no repositório quais associações vir junto. O Spring Data monta o JOIN; o "
                    + "código da regra de negócio não muda.";
            case JOIN_FETCH -> "O mesmo resultado escrito à mão em JPQL. Dá controle total sobre a consulta.";
            case PROJECAO -> "Não carrega entidades: o SELECT já devolve só as colunas que a tela mostra, direto "
                    + "em um record. A mais enxuta para telas de leitura.";
        };
    }

    /**
     * Cria uma aula descartável com poucas vagas, dispara {@code concorrentes} threads reservando ao mesmo tempo
     * e confere no banco que nunca houve mais confirmados do que vagas.
     */
    public Disputa disputar(int concorrentes, int vagas) {
        int n = Math.max(2, Math.min(concorrentes, 40));
        int v = Math.max(1, Math.min(vagas, n - 1));

        List<Long> alunoIds = tx.execute(s -> alunos.findAll(PageRequest.of(0, n)).map(Aluno::getId).getContent());
        if (alunoIds.size() < n) {
            throw new Erros.RegraDeNegocio("A base tem poucos alunos para essa disputa.");
        }
        Long aulaId = tx.execute(s -> {
            var unidade = unidades.findAllByOrderByNome().get(0);
            var instrutor = instrutores.findByUnidadeId(unidade.getId()).get(0);
            var modalidade = modalidades.findAll().get(0);
            return aulas.save(new Aula(modalidade, unidade, instrutor, LocalDateTime.now(relogio).plusDays(30), 45, v))
                    .getId();
        });

        int antes = reservaService.conflitosResolvidos();
        long inicio = System.nanoTime();
        int semResposta = 0;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<?>> futuros = new ArrayList<>();
            for (Long alunoId : alunoIds) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return reservaService.reservarSemRegras(aulaId, alunoId);
                }));
            }
            largada.countDown();
            for (Future<?> f : futuros) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    semResposta++;
                }
            }
        } finally {
            pool.shutdownNow();
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        long confirmadas = reservas.countByAulaIdAndStatus(aulaId, StatusReserva.CONFIRMADA);
        long espera = reservas.countByAulaIdAndStatus(aulaId, StatusReserva.ESPERA);
        tx.executeWithoutResult(s -> aulas.deleteById(aulaId));

        return new Disputa(n, v, confirmadas, espera, semResposta, reservaService.conflitosResolvidos() - antes, ms,
                confirmadas > v);
    }
}
