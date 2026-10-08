package dev.barboza.pulso.servico;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.ContatoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.MensalidadeRepository;
import dev.barboza.pulso.repositorio.Resumos.Atraso;
import dev.barboza.pulso.repositorio.Resumos.Frequencia;
import dev.barboza.pulso.repositorio.Resumos.UltimoContato;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Nivel;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Resultado;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Sinais;

/**
 * Risco de evasão da base inteira em três consultas (matrículas ativas, frequência agregada, atrasos),
 * não importa se são 200 ou 200 mil alunos: o cálculo em si roda em memória sobre os resultados.
 */
@Service
@Transactional(readOnly = true)
public class RiscoService {

    public record AlunoEmRisco(Long alunoId, String nome, Long unidadeId, String unidade, String plano,
            BigDecimal valorMensal, int pontos, Nivel nivel, List<String> fatores, Long diasSemTreinar,
            Long diasDesdeContato, String ultimoResultado) {
    }

    private final MatriculaRepository matriculas;
    private final AcessoRepository acessos;
    private final MensalidadeRepository mensalidades;
    private final ContatoRepository contatos;
    private final Clock relogio;

    public RiscoService(MatriculaRepository matriculas, AcessoRepository acessos, MensalidadeRepository mensalidades,
            ContatoRepository contatos, Clock relogio) {
        this.matriculas = matriculas;
        this.acessos = acessos;
        this.mensalidades = mensalidades;
        this.contatos = contatos;
        this.relogio = relogio;
    }

    /** Ranking do mais para o menos arriscado; só entra quem tem algum sinal (nota maior que zero). */
    public List<AlunoEmRisco> ranking(Long unidadeId) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        Map<Long, Frequencia> frequencia = new HashMap<>();
        for (Frequencia f : acessos.frequenciaPorAluno(agora.minusDays(14), agora.minusDays(28))) {
            frequencia.put(f.alunoId(), f);
        }
        Map<Long, LocalDate> atrasos = new HashMap<>();
        for (Atraso a : mensalidades.vencimentoMaisAntigoPorAluno(hoje)) {
            atrasos.put(a.alunoId(), a.vencimentoMaisAntigo());
        }

        Map<Long, UltimoContato> ultimos = new HashMap<>();
        contatos.ultimoDeCadaAluno().forEach(c -> ultimos.put(c.alunoId(), c));

        return matriculas.ativasComAlunoEPlano().stream()
                .filter(m -> unidadeId == null || m.getAluno().getUnidade().getId().equals(unidadeId))
                .map(m -> avaliar(m, frequencia.get(m.getAluno().getId()), atrasos.get(m.getAluno().getId()),
                        ultimos.get(m.getAluno().getId()), agora))
                .filter(r -> r.pontos() > 0)
                .sorted(Comparator.comparingInt(AlunoEmRisco::pontos).reversed()
                        .thenComparing(AlunoEmRisco::nome))
                .toList();
    }

    /** Nota de um aluno só (para a ficha): quatro consultas pequenas em vez da base inteira. */
    public Resultado deAluno(Long alunoId, LocalDate inicioDaMatricula) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();
        LocalDateTime ultima = acessos.ultimaVisitaDoAluno(alunoId);
        Long diasSemTreinar = ultima == null || ultima.isBefore(agora.minusDays(28)) ? null
                : ChronoUnit.DAYS.between(ultima, agora);
        long recentes = acessos.visitasDoAlunoDesde(alunoId, agora.minusDays(14));
        long anteriores = acessos.visitasDoAlunoDesde(alunoId, agora.minusDays(28)) - recentes;
        LocalDate vencimento = mensalidades.vencimentoMaisAntigoDoAluno(alunoId, hoje);
        long atraso = vencimento == null ? 0 : ChronoUnit.DAYS.between(vencimento, hoje);
        return PontuacaoDeRisco.avaliar(new Sinais(diasSemTreinar, recentes, anteriores, atraso,
                ChronoUnit.DAYS.between(inicioDaMatricula, hoje)));
    }

    private AlunoEmRisco avaliar(Matricula m, Frequencia f, LocalDate vencimentoAntigo, UltimoContato contato,
            LocalDateTime agora) {
        Long diasSemTreinar = f == null ? null : ChronoUnit.DAYS.between(f.ultimaVisita(), agora);
        long atraso = vencimentoAntigo == null ? 0 : ChronoUnit.DAYS.between(vencimentoAntigo, agora.toLocalDate());
        long diasDeCasa = ChronoUnit.DAYS.between(m.getInicio(), agora.toLocalDate());
        Resultado r = PontuacaoDeRisco.avaliar(new Sinais(diasSemTreinar, f == null ? 0 : f.ultimas2Semanas(),
                f == null ? 0 : f.duasSemanasAntes(), atraso, diasDeCasa));
        var a = m.getAluno();
        return new AlunoEmRisco(a.getId(), a.getNome(), a.getUnidade().getId(), a.getUnidade().getNome(),
                m.getPlano().getNome(), m.getPlano().getValorMensal(), r.pontos(), r.nivel(), r.fatores(),
                diasSemTreinar, contato == null ? null : ChronoUnit.DAYS.between(contato.feitoEm(), agora),
                contato == null ? null : contato.resultado().name());
    }
}
