package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Aula;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.Reserva;
import dev.barboza.pulso.dominio.StatusReserva;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.AulaRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.ReservaRepository;

/**
 * Agenda de aulas e reservas com lista de espera.
 *
 * <p>A reserva concorrente é resolvida com bloqueio otimista (@Version na Aula): em vez de travar a linha
 * enquanto alguém decide, cada transação trabalha livre e só o commit confere se a aula mudou. Quem perde a
 * corrida refaz a operação enxergando o estado novo. Por isso o método público não é @Transactional:
 * cada tentativa é uma transação nova (TransactionTemplate), senão o retry reutilizaria a sessão suja.
 */
@Service
public class ReservaService {

    /** Com dezenas de threads na mesma aula, um perdedor pode precisar de muitas rodadas; cada falha é barata. */
    private static final int TENTATIVAS = 80;

    public record AulaVisao(Long id, Long modalidadeId, String modalidade, String icone, Long unidadeId,
            String unidade, String instrutor, LocalDateTime inicio, int duracaoMin, int vagas, long confirmadas,
            long espera, boolean reservada) {
    }

    public record ResultadoDaReserva(Long reservaId, String status, int posicaoNaFila, int tentativas) {
    }

    public record Cancelamento(boolean promoveuAlguem, String promovido) {
    }

    private final AulaRepository aulas;
    private final ReservaRepository reservas;
    private final AlunoRepository alunos;
    private final MatriculaRepository matriculas;
    private final TransactionTemplate tx;
    private final Clock relogio;
    private final AtomicInteger conflitosResolvidos = new AtomicInteger();

    public ReservaService(AulaRepository aulas, ReservaRepository reservas, AlunoRepository alunos,
            MatriculaRepository matriculas, PlatformTransactionManager gerenciador, Clock relogio) {
        this.aulas = aulas;
        this.reservas = reservas;
        this.alunos = alunos;
        this.matriculas = matriculas;
        this.tx = new TransactionTemplate(gerenciador);
        this.relogio = relogio;
    }

    /** Total de conflitos de versão que o sistema já resolveu refazendo a operação (vai para o painel). */
    public int conflitosResolvidos() {
        return conflitosResolvidos.get();
    }

    @Transactional(readOnly = true)
    public List<AulaVisao> agenda(Long unidadeId, LocalDate dia, Long alunoId) {
        LocalDateTime de = dia.atStartOfDay();
        List<Aula> lista = aulas.daAgenda(unidadeId, de, de.plusDays(1));
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(Aula::getId).toList();
        Map<Long, long[]> contagem = new HashMap<>();
        for (Object[] linha : reservas.contagemPorAula(ids)) {
            long[] c = contagem.computeIfAbsent((Long) linha[0], k -> new long[2]);
            c[linha[1] == StatusReserva.CONFIRMADA ? 0 : 1] = (Long) linha[2];
        }
        List<Long> doAluno = alunoId == null ? List.of() : reservas.aulasDoAluno(alunoId, ids);
        List<AulaVisao> saida = new ArrayList<>();
        for (Aula a : lista) {
            long[] c = contagem.getOrDefault(a.getId(), new long[2]);
            saida.add(new AulaVisao(a.getId(), a.getModalidade().getId(), a.getModalidade().getNome(),
                    a.getModalidade().getIcone(), a.getUnidade().getId(), a.getUnidade().getNome(),
                    a.getInstrutor().getNome(), a.getInicio(), a.getDuracaoMin(), a.getVagas(), c[0], c[1],
                    doAluno.contains(a.getId())));
        }
        return saida;
    }

    /** Reserva respeitando as regras do plano; entra na fila de espera se a aula estiver cheia. */
    public ResultadoDaReserva reservar(Long aulaId, Long alunoId) {
        tx.executeWithoutResult(s -> validarElegibilidade(aulaId, alunoId));
        return reservarSemRegras(aulaId, alunoId);
    }

    /** Roda dentro da transação aberta por quem chama (as associações lazy precisam da sessão). */
    private void validarElegibilidade(Long aulaId, Long alunoId) {
        Aula aula = aulas.buscarCompleta(aulaId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Aula " + aulaId + " não encontrada."));
        Aluno aluno = alunos.buscarComUnidade(alunoId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado."));
        if (aula.getInicio().isBefore(LocalDateTime.now(relogio))) {
            throw new Erros.RegraDeNegocio("Essa aula já começou.");
        }
        Matricula m = matriculas.buscarVigente(alunoId).filter(Matricula::ativa).orElseThrow(
                () -> new Erros.RegraDeNegocio(aluno.getNome() + " não tem matrícula ativa para reservar."));
        if (!m.getPlano().inclui(aula.getModalidade())) {
            throw new Erros.RegraDeNegocio("O plano " + m.getPlano().getNome() + " não inclui "
                    + aula.getModalidade().getNome() + ".");
        }
        if (!m.getPlano().isMultiUnidade() && !aluno.getUnidade().getId().equals(aula.getUnidade().getId())) {
            throw new Erros.RegraDeNegocio("O plano " + m.getPlano().getNome() + " vale só na unidade "
                    + aluno.getUnidade().getNome() + ".");
        }
    }

    /** O núcleo concorrente, sem regras de plano: usado pela reserva e pela demonstração do laboratório. */
    public ResultadoDaReserva reservarSemRegras(Long aulaId, Long alunoId) {
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                final int numero = tentativa;
                return tx.execute(status -> {
                    Aula aula = aulas.buscarParaReservar(aulaId)
                            .orElseThrow(() -> new Erros.NaoEncontrado("Aula " + aulaId + " não encontrada."));
                    Aluno aluno = alunos.findById(alunoId)
                            .orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado."));
                    Reserva r;
                    try {
                        r = aula.reservar(aluno, LocalDateTime.now(relogio));
                    } catch (IllegalStateException e) {
                        throw new Erros.RegraDeNegocio(e.getMessage());
                    }
                    reservas.saveAndFlush(r);
                    int fila = r.getStatus() == StatusReserva.ESPERA ? (int) aula.naEspera() : 0;
                    return new ResultadoDaReserva(r.getId(), r.getStatus().name(), fila, numero);
                });
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
                conflitosResolvidos.incrementAndGet(); // alguém reservou no mesmo instante: refaz com o estado novo
                esperarUmPouco(tentativa);
            }
        }
        throw new Erros.RegraDeNegocio("A aula está muito disputada agora. Tente novamente em instantes.");
    }

    /** Recuo com sorteio (jitter): evita que os perdedores voltem todos no mesmo instante e colidam de novo. */
    private static void esperarUmPouco(int tentativa) {
        try {
            Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 3L + Math.min(tentativa, 10) * 2));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Cancela e promove o primeiro da fila de espera, com a mesma proteção contra corrida. */
    public Cancelamento cancelar(Long reservaId) {
        Long aulaId = tx.execute(s -> reservas.findById(reservaId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Reserva " + reservaId + " não encontrada."))
                .getAula().getId());
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                return tx.execute(status -> {
                    Aula aula = aulas.buscarParaReservar(aulaId).orElseThrow();
                    Reserva alvo = aula.getReservas().stream().filter(r -> r.getId().equals(reservaId)).findFirst()
                            .orElseThrow(() -> new Erros.NaoEncontrado("Reserva " + reservaId + " não encontrada."));
                    if (alvo.getStatus() == StatusReserva.CANCELADA) {
                        throw new Erros.RegraDeNegocio("Essa reserva já foi cancelada.");
                    }
                    Reserva promovida = aula.cancelar(alvo);
                    return new Cancelamento(promovida != null,
                            promovida == null ? null : promovida.getAluno().getNome());
                });
            } catch (ObjectOptimisticLockingFailureException e) {
                conflitosResolvidos.incrementAndGet();
                esperarUmPouco(tentativa);
            }
        }
        throw new Erros.RegraDeNegocio("Não foi possível cancelar agora. Tente novamente.");
    }
}
