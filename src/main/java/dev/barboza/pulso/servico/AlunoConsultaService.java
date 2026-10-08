package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.ContatoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.MensalidadeRepository;
import dev.barboza.pulso.repositorio.ReservaRepository;
import dev.barboza.pulso.servico.Visoes.AvaliacaoVisao;
import dev.barboza.pulso.servico.Visoes.ContatoVisao;
import dev.barboza.pulso.servico.Visoes.Ficha;
import dev.barboza.pulso.servico.Visoes.FrequenciaVisao;
import dev.barboza.pulso.servico.Visoes.MatriculaVisao;
import dev.barboza.pulso.servico.Visoes.MensalidadeVisao;
import dev.barboza.pulso.servico.Visoes.ReservaVisao;
import dev.barboza.pulso.servico.Visoes.ResumoDoAluno;
import dev.barboza.pulso.servico.Visoes.RiscoVisao;

/** Leitura de alunos: lista paginada e ficha completa, cada uma com um número fixo de consultas. */
@Service
@Transactional(readOnly = true)
public class AlunoConsultaService {

    private final AlunoService alunos;
    private final MatriculaRepository matriculas;
    private final MensalidadeRepository mensalidades;
    private final AcessoRepository acessos;
    private final ReservaRepository reservas;
    private final RiscoService risco;
    private final ContatoRepository contatos;
    private final Clock relogio;

    public AlunoConsultaService(AlunoService alunos, MatriculaRepository matriculas,
            MensalidadeRepository mensalidades, AcessoRepository acessos, ReservaRepository reservas,
            RiscoService risco, ContatoRepository contatos, Clock relogio) {
        this.alunos = alunos;
        this.matriculas = matriculas;
        this.mensalidades = mensalidades;
        this.acessos = acessos;
        this.reservas = reservas;
        this.risco = risco;
        this.contatos = contatos;
        this.relogio = relogio;
    }

    /** Página de alunos: 1 consulta da página (com a unidade), 1 de contagem e 1 para as matrículas vigentes. */
    public Page<ResumoDoAluno> listar(String busca, Long unidadeId, String bairro, StatusMatricula situacao,
            Pageable pagina) {
        Page<Aluno> pag = alunos.listar(busca, unidadeId, bairro, situacao, pagina);
        Map<Long, Matricula> vigentes = new HashMap<>();
        if (!pag.isEmpty()) {
            matriculas.vigentesDosAlunos(pag.map(Aluno::getId).getContent())
                    .forEach(m -> vigentes.put(m.getAluno().getId(), m));
        }
        LocalDate hoje = LocalDate.now(relogio);
        return pag.map(a -> mascarar(resumo(a, vigentes.get(a.getId()), hoje)));
    }

    public Ficha ficha(Long id) {
        Aluno a = alunos.buscar(id);
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        List<Matricula> historico = matriculas.doAluno(id);
        Matricula vigente = historico.stream().filter(Matricula::vigente).findFirst().orElse(null);

        List<MatriculaVisao> mats = historico.stream().map(m -> new MatriculaVisao(m.getId(), m.getPlano().getId(),
                m.getPlano().getNome(), m.getPlano().getValorMensal(), m.getInicio(), m.getFim(),
                m.getStatus().name(), m.getMotivoCancelamento())).toList();

        List<MensalidadeVisao> cobrancas = mensalidades.doAluno(id).stream().limit(12).map(m -> {
            var p = m.getPagamento();
            return new MensalidadeVisao(m.getId(), m.getCompetencia(), m.getValor(), m.getVencimento(),
                    m.situacao(hoje).name(), m.diasDeAtraso(hoje), p == null ? null : p.forma(),
                    p == null ? null : p.referencia(), p == null ? null : p.getPagoEm());
        }).toList();

        List<AvaliacaoVisao> avaliacoes = alunos.avaliacoesDoAluno(id).stream()
                .map(v -> new AvaliacaoVisao(v.getId(), v.getData(), v.getPeso(), v.getAltura(), v.imc(),
                        v.faixaDoImc(), v.getPercentualGordura(), v.getCinturaCm()))
                .toList();

        List<ReservaVisao> futuras = reservas.futurasDoAluno(id, agora).stream()
                .map(r -> new ReservaVisao(r.getId(), r.getAula().getId(), r.getAula().getModalidade().getNome(),
                        r.getAula().getModalidade().getIcone(), r.getAula().getUnidade().getNome(),
                        r.getAula().getInicio(), r.getStatus().name()))
                .toList();

        return new Ficha(resumo(a, vigente, hoje), a.getNascimento(), mats, cobrancas, avaliacoes,
                frequencia(id, agora), risco(id, vigente), futuras,
                contatos.findByAlunoIdOrderByFeitoEmDesc(id).stream().map(c -> new ContatoVisao(c.getId(),
                        c.getCanal().name(), c.getResultado().name(), c.getObservacao(), c.getFeitoEm(),
                        c.getRiscoNaEpoca())).toList());
    }

    private FrequenciaVisao frequencia(Long alunoId, LocalDateTime agora) {
        List<LocalDateTime> visitas = acessos.visitasDoAluno(alunoId, agora.minusWeeks(8));
        int[] semanas = new int[8];
        for (LocalDateTime v : visitas) {
            long atras = ChronoUnit.DAYS.between(v, agora) / 7;
            semanas[(int) (7 - Math.min(7, atras))]++;
        }
        long ult30 = visitas.stream().filter(v -> v.isAfter(agora.minusDays(30))).count();
        LocalDateTime ultima = acessos.ultimaVisitaDoAluno(alunoId);
        Long dias = ultima == null ? null : ChronoUnit.DAYS.between(ultima, agora);
        return new FrequenciaVisao(ult30, dias, ultima, java.util.Arrays.stream(semanas).boxed().toList());
    }

    private RiscoVisao risco(Long alunoId, Matricula vigente) {
        if (vigente == null || !vigente.ativa()) {
            return null;
        }
        var r = risco.deAluno(alunoId, vigente.getInicio());
        return new RiscoVisao(r.pontos(), r.nivel().name(), r.fatores());
    }

    /** LGPD: listas mostram só os dígitos do meio do CPF. O número completo fica na ficha do aluno. */
    private static ResumoDoAluno mascarar(ResumoDoAluno r) {
        String cpf = r.cpf() == null || r.cpf().length() < 14 ? r.cpf()
                : "***." + r.cpf().substring(4, 11) + "-**";
        return new ResumoDoAluno(r.id(), r.nome(), cpf, r.email(), r.bairro(), r.cidade(), r.uf(), r.unidadeId(),
                r.unidade(), r.situacao(), r.plano(), r.idade());
    }

    private static ResumoDoAluno resumo(Aluno a, Matricula m, LocalDate hoje) {
        String situacao = m == null ? StatusMatricula.CANCELADA.name() : m.getStatus().name();
        return new ResumoDoAluno(a.getId(), a.getNome(), a.getCpf(), a.getEmail(), a.getEndereco().getBairro(),
                a.getEndereco().getCidade(), a.getEndereco().getUf(), a.getUnidade().getId(),
                a.getUnidade().getNome(), situacao, m == null ? null : m.getPlano().getNome(), a.idade(hoje));
    }
}
