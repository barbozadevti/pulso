package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.Mensalidade;
import dev.barboza.pulso.dominio.Pagamento;
import dev.barboza.pulso.dominio.PagamentoBoleto;
import dev.barboza.pulso.dominio.PagamentoCartao;
import dev.barboza.pulso.dominio.PagamentoPix;
import dev.barboza.pulso.dominio.Plano;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.MensalidadeRepository;
import dev.barboza.pulso.repositorio.PlanoRepository;

/** Ciclo de vida da matrícula e da cobrança. */
@Service
@Transactional
public class MatriculaService {

    /** Dia do mês em que a mensalidade vence. */
    public static final int DIA_DE_VENCIMENTO = 10;

    private final AlunoRepository alunos;
    private final PlanoRepository planos;
    private final MatriculaRepository matriculas;
    private final MensalidadeRepository mensalidades;
    private final Clock relogio;

    public MatriculaService(AlunoRepository alunos, PlanoRepository planos, MatriculaRepository matriculas,
            MensalidadeRepository mensalidades, Clock relogio) {
        this.alunos = alunos;
        this.planos = planos;
        this.matriculas = matriculas;
        this.mensalidades = mensalidades;
        this.relogio = relogio;
    }

    public Matricula matricular(Long alunoId, Long planoId) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado."));
        Plano plano = plano(planoId);
        if (matriculas.buscarVigente(alunoId).isPresent()) {
            throw new Erros.RegraDeNegocio(aluno.getNome() + " já tem uma matrícula em andamento.");
        }
        LocalDate hoje = LocalDate.now(relogio);
        Matricula m = matriculas.save(new Matricula(aluno, plano, hoje));
        // A primeira mensalidade vence em 5 dias (ou no dia padrão, o que for mais distante dentro do mês).
        m.gerarMensalidade(hoje, hoje.plusDays(5));
        return m;
    }

    public Matricula cancelar(Long matriculaId, String motivo) {
        Matricula m = buscar(matriculaId);
        try {
            m.cancelar(LocalDate.now(relogio), motivo == null || motivo.isBlank() ? "Não informado" : motivo);
        } catch (IllegalStateException e) {
            throw new Erros.RegraDeNegocio(e.getMessage());
        }
        return m; // dirty checking: o UPDATE sai no commit
    }

    public Matricula trancar(Long matriculaId) {
        Matricula m = buscar(matriculaId);
        try {
            m.trancar();
        } catch (IllegalStateException e) {
            throw new Erros.RegraDeNegocio(e.getMessage());
        }
        return m;
    }

    public Matricula reativar(Long matriculaId) {
        Matricula m = buscar(matriculaId);
        try {
            m.reativar();
        } catch (IllegalStateException e) {
            throw new Erros.RegraDeNegocio(e.getMessage());
        }
        return m;
    }

    public Matricula mudarPlano(Long matriculaId, Long planoId) {
        Matricula m = buscar(matriculaId);
        if (!m.vigente()) {
            throw new Erros.RegraDeNegocio("Não dá para mudar o plano de uma matrícula cancelada.");
        }
        m.mudarPlano(plano(planoId));
        return m;
    }

    // ---- cobrança

    public enum Forma { PIX, CARTAO, BOLETO }

    public Mensalidade pagar(Long mensalidadeId, Forma forma, String referencia) {
        Mensalidade m = mensalidades.findById(mensalidadeId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Mensalidade " + mensalidadeId + " não encontrada."));
        if (m.paga()) {
            throw new Erros.RegraDeNegocio("A mensalidade de " + m.getCompetencia() + " já está paga.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        Pagamento p = switch (forma) {
            case PIX -> new PagamentoPix(m.getValor(), agora, referencia == null || referencia.isBlank()
                    ? "PULSO" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase()
                    : referencia);
            case CARTAO -> new PagamentoCartao(m.getValor(), agora, finalDoCartao(referencia));
            case BOLETO -> new PagamentoBoleto(m.getValor(), agora, referencia == null || referencia.isBlank()
                    ? "23793.38128 60000.000003 00000.000400 1 " + m.getValor().movePointRight(2).toPlainString()
                    : referencia);
        };
        m.pagar(p);
        return m;
    }

    public record Atrasada(Long id, Long alunoId, String aluno, String plano, LocalDate competencia,
            LocalDate vencimento, java.math.BigDecimal valor, long diasDeAtraso) {
    }

    public record LinhaDaMatricula(Long id, Long alunoId, String aluno, String bairro, String plano, LocalDate inicio,
            String status) {
    }

    @Transactional(readOnly = true)
    public List<Atrasada> atrasadas() {
        LocalDate hoje = LocalDate.now(relogio);
        return mensalidades.atrasadas(hoje).stream()
                .map(m -> new Atrasada(m.getId(), m.getMatricula().getAluno().getId(),
                        m.getMatricula().getAluno().getNome(), m.getMatricula().getPlano().getNome(),
                        m.getCompetencia(), m.getVencimento(), m.getValor(), m.diasDeAtraso(hoje)))
                .toList();
    }

    /** A consulta do desafio original (matrículas por bairro do aluno; sem bairro, as ativas), agora sobre o modelo completo. */
    @Transactional(readOnly = true)
    public List<LinhaDaMatricula> porBairro(String bairro) {
        List<Matricula> lista = bairro == null || bairro.isBlank() ? matriculas.ativasComAlunoEPlano()
                : matriculas.buscarPorBairroDoAluno(bairro);
        return lista.stream().map(m -> new LinhaDaMatricula(m.getId(), m.getAluno().getId(), m.getAluno().getNome(),
                m.getAluno().getEndereco().getBairro(), m.getPlano().getNome(), m.getInicio(), m.getStatus().name()))
                .toList();
    }

    /**
     * Fecha o mês: gera a mensalidade do mês corrente para toda matrícula ativa que ainda não tem.
     * Roda sozinha todo dia 1º às 02:00 e é idempotente (pode ser chamada de novo sem duplicar).
     */
    @Scheduled(cron = "0 0 2 1 * *", zone = "America/Sao_Paulo")
    public int gerarMensalidadesDoMes() {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate vencimento = hoje.withDayOfMonth(DIA_DE_VENCIMENTO);
        int geradas = 0;
        for (Matricula m : matriculas.ativasComAlunoEPlano()) {
            int antes = m.getMensalidades().size();
            m.gerarMensalidade(hoje, vencimento);
            geradas += m.getMensalidades().size() - antes;
        }
        return geradas;
    }

    private Matricula buscar(Long id) {
        return matriculas.findById(id).orElseThrow(() -> new Erros.NaoEncontrado("Matrícula " + id + " não encontrada."));
    }

    private Plano plano(Long id) {
        return planos.findById(id).orElseThrow(() -> new Erros.NaoEncontrado("Plano " + id + " não encontrado."));
    }

    private static String finalDoCartao(String referencia) {
        String d = referencia == null ? "" : referencia.replaceAll("\\D", "");
        return d.length() >= 4 ? d.substring(d.length() - 4) : "0000";
    }
}
