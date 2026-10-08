package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Acesso;
import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.dominio.Unidade;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.MensalidadeRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;

/** A decisão da catraca: libera ou nega, sempre dizendo o motivo em linguagem de recepção. */
@Service
@Transactional
public class CatracaService {

    /** Dias de tolerância depois do vencimento antes de a catraca bloquear. */
    public static final int TOLERANCIA_DE_ATRASO = 10;

    public record Decisao(boolean liberado, String motivo, Long alunoId, String aluno, Long acessoId,
            long ocupacao, int capacidade) {
    }

    public record Presente(Long acessoId, Long alunoId, String nome, LocalDateTime entrada, long minutos) {
    }

    public record AoVivo(Long unidadeId, String unidade, long ocupacao, int capacidade, List<Presente> presentes) {
    }

    private final AlunoRepository alunos;
    private final UnidadeRepository unidades;
    private final MatriculaRepository matriculas;
    private final MensalidadeRepository mensalidades;
    private final AcessoRepository acessos;
    private final Clock relogio;

    public CatracaService(AlunoRepository alunos, UnidadeRepository unidades, MatriculaRepository matriculas,
            MensalidadeRepository mensalidades, AcessoRepository acessos, Clock relogio) {
        this.alunos = alunos;
        this.unidades = unidades;
        this.matriculas = matriculas;
        this.mensalidades = mensalidades;
        this.acessos = acessos;
        this.relogio = relogio;
    }

    /** Entrada pelo CPF (leitor de QR ou digitado). */
    public Decisao entrar(String cpf, Long unidadeId) {
        String formatado = Cpf.valido(cpf) ? Cpf.formatar(cpf) : cpf;
        Aluno aluno = alunos.findByCpf(formatado)
                .orElseThrow(() -> new Erros.NaoEncontrado("Nenhum aluno com o CPF informado."));
        return entrar(aluno, unidadeId);
    }

    public Decisao entrarPorId(Long alunoId, Long unidadeId) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado."));
        return entrar(aluno, unidadeId);
    }

    private Decisao entrar(Aluno aluno, Long unidadeId) {
        Unidade unidade = unidades.findById(unidadeId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Unidade " + unidadeId + " não encontrada."));
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();
        long dentro = acessos.countByUnidadeIdAndSaidaIsNull(unidadeId);

        String motivo = motivoDaNegativa(aluno, unidade, hoje, dentro);
        if (motivo != null) {
            return new Decisao(false, motivo, aluno.getId(), aluno.getNome(), null, dentro, unidade.getCapacidade());
        }
        Acesso acesso = acessos.save(new Acesso(aluno, unidade, agora));
        return new Decisao(true, "Bom treino, " + primeiroNome(aluno) + "!", aluno.getId(), aluno.getNome(),
                acesso.getId(), dentro + 1, unidade.getCapacidade());
    }

    private String motivoDaNegativa(Aluno aluno, Unidade unidade, LocalDate hoje, long dentro) {
        Matricula m = matriculas.buscarVigente(aluno.getId()).orElse(null);
        if (m == null) {
            return "Sem matrícula ativa. Procure a recepção para renovar.";
        }
        if (m.getStatus() == StatusMatricula.TRANCADA) {
            return "Matrícula trancada. Reative na recepção para voltar a treinar.";
        }
        if (!m.getPlano().isMultiUnidade() && !aluno.getUnidade().getId().equals(unidade.getId())) {
            return "O plano " + m.getPlano().getNome() + " vale só na unidade " + aluno.getUnidade().getNome() + ".";
        }
        LocalDate vencimento = mensalidades.vencimentoMaisAntigoDoAluno(aluno.getId(), hoje);
        if (vencimento != null) {
            long atraso = ChronoUnit.DAYS.between(vencimento, hoje);
            if (atraso > TOLERANCIA_DE_ATRASO) {
                return "Mensalidade vencida há " + atraso + " dias. Regularize para liberar o acesso.";
            }
        }
        if (acessos.findFirstByAlunoIdAndSaidaIsNullOrderByEntradaDesc(aluno.getId()).isPresent()) {
            return "Você já está dentro da academia. Registre a saída antes de entrar de novo.";
        }
        if (dentro >= unidade.getCapacidade()) {
            return "Unidade lotada (" + dentro + " pessoas). Tente em alguns minutos.";
        }
        return null;
    }

    public Decisao sair(Long alunoId) {
        Acesso acesso = acessos.findFirstByAlunoIdAndSaidaIsNullOrderByEntradaDesc(alunoId)
                .orElseThrow(() -> new Erros.RegraDeNegocio("Este aluno não está dentro de nenhuma unidade."));
        acesso.sair(LocalDateTime.now(relogio));
        Unidade u = acesso.getUnidade();
        long dentro = acessos.countByUnidadeIdAndSaidaIsNull(u.getId());
        return new Decisao(true, "Até a próxima, " + primeiroNome(acesso.getAluno()) + "!", alunoId,
                acesso.getAluno().getNome(), acesso.getId(), dentro, u.getCapacidade());
    }

    @Transactional(readOnly = true)
    public AoVivo aoVivo(Long unidadeId) {
        Unidade u = unidades.findById(unidadeId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Unidade " + unidadeId + " não encontrada."));
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Presente> presentes = acessos.dentroDaUnidade(unidadeId).stream()
                .map(a -> new Presente(a.getId(), a.getAluno().getId(), a.getAluno().getNome(), a.getEntrada(),
                        ChronoUnit.MINUTES.between(a.getEntrada(), agora)))
                .toList();
        return new AoVivo(u.getId(), u.getNome(), presentes.size(), u.getCapacidade(), presentes);
    }

    private static String primeiroNome(Aluno a) {
        return a.getNome().split(" ")[0];
    }
}
