package dev.barboza.pulso.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.barboza.pulso.dominio.Acesso;
import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.Aula;
import dev.barboza.pulso.dominio.CanalDeContato;
import dev.barboza.pulso.dominio.Contato;
import dev.barboza.pulso.dominio.ResultadoDoContato;
import dev.barboza.pulso.dominio.AvaliacaoFisica;
import dev.barboza.pulso.dominio.Endereco;
import dev.barboza.pulso.dominio.Instrutor;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.Mensalidade;
import dev.barboza.pulso.dominio.Modalidade;
import dev.barboza.pulso.dominio.Pagamento;
import dev.barboza.pulso.dominio.PagamentoBoleto;
import dev.barboza.pulso.dominio.PagamentoCartao;
import dev.barboza.pulso.dominio.PagamentoPix;
import dev.barboza.pulso.dominio.Plano;
import dev.barboza.pulso.dominio.Unidade;
import dev.barboza.pulso.repositorio.AcessoRepository;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.AulaRepository;
import dev.barboza.pulso.repositorio.ContatoRepository;
import dev.barboza.pulso.repositorio.InstrutorRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;
import dev.barboza.pulso.repositorio.ModalidadeRepository;
import dev.barboza.pulso.repositorio.PlanoRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;
import dev.barboza.pulso.api.AutenticacaoController;
import dev.barboza.pulso.seguranca.Usuario;
import dev.barboza.pulso.seguranca.UsuarioRepository;
import dev.barboza.pulso.servico.Cpf;

/**
 * Uma rede fictícia de academias com seis meses de história: alunos, matrículas, cobranças (algumas em atraso),
 * avaliações físicas, catraca e a agenda dos próximos dias. Tudo gerado com semente fixa, então a demonstração
 * é a mesma a cada execução. Só roda se o banco estiver vazio.
 */
@Component
@ConditionalOnProperty(name = "pulso.demo", havingValue = "true")
public class DadosDeDemonstracao implements ApplicationRunner {

    private static final String[] NOMES = { "Ana", "Bruno", "Carla", "Diego", "Elisa", "Felipe", "Gabriela", "Hugo",
            "Isabela", "João", "Karina", "Lucas", "Mariana", "Nicolas", "Olívia", "Paulo", "Queila", "Rafael", "Sabrina",
            "Thiago", "Ursula", "Vítor", "Wanda", "Xavier", "Yasmin", "Zeca", "Aline", "Bernardo", "Camila", "Daniel",
            "Eduarda", "Fábio", "Giovana", "Henrique", "Letícia", "Matheus", "Natália", "Otávio", "Patrícia", "Renato" };
    private static final String[] SOBRENOMES = { "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira",
            "Alves", "Pereira", "Lima", "Gomes", "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes",
            "Soares", "Fernandes", "Vieira", "Barbosa", "Rocha", "Dias", "Nascimento", "Andrade", "Moreira", "Nunes",
            "Marques", "Machado", "Mendes", "Freitas", "Cardoso", "Ramos", "Teixeira", "Moura", "Cavalcanti" };

    private record Local(String nome, String cidade, String uf, int capacidade, String[] bairros) {
    }

    private static final List<Local> LOCAIS = List.of(
            new Local("Pulso Praia do Canto", "Vitória", "ES", 90,
                    new String[] { "Praia do Canto", "Jardim da Penha", "Enseada do Suá", "Mata da Praia", "Jardim Camburi" }),
            new Local("Pulso Pinheiros", "São Paulo", "SP", 140,
                    new String[] { "Pinheiros", "Vila Madalena", "Jardins", "Perdizes", "Itaim Bibi" }),
            new Local("Pulso Savassi", "Belo Horizonte", "MG", 110,
                    new String[] { "Savassi", "Funcionários", "Lourdes", "Santo Agostinho", "Serra" }),
            new Local("Pulso Boa Viagem", "Recife", "PE", 100,
                    new String[] { "Boa Viagem", "Pina", "Setúbal", "Imbiribeira", "Madalena" }));

    private final TransactionTemplate tx;
    private final Clock relogio;
    private final UnidadeRepository unidades;
    private final ModalidadeRepository modalidades;
    private final PlanoRepository planos;
    private final InstrutorRepository instrutores;
    private final AlunoRepository alunos;
    private final MatriculaRepository matriculas;
    private final AcessoRepository acessos;
    private final AulaRepository aulas;
    private final ContatoRepository contatos;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public DadosDeDemonstracao(PlatformTransactionManager gerenciador, Clock relogio, UnidadeRepository unidades,
            ModalidadeRepository modalidades, PlanoRepository planos, InstrutorRepository instrutores,
            AlunoRepository alunos, MatriculaRepository matriculas, AcessoRepository acessos, AulaRepository aulas,
            ContatoRepository contatos, UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.tx = new TransactionTemplate(gerenciador);
        this.relogio = relogio;
        this.unidades = unidades;
        this.modalidades = modalidades;
        this.planos = planos;
        this.instrutores = instrutores;
        this.alunos = alunos;
        this.matriculas = matriculas;
        this.acessos = acessos;
        this.aulas = aulas;
        this.contatos = contatos;
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (unidades.count() > 0) {
            return;
        }
        tx.executeWithoutResult(s -> popular());
    }

    // ---------------------------------------------------------------------------------------------

    private enum Perfil { ASSIDUO, REGULAR, OCASIONAL, EVADINDO }

    /** Alunos que pararam de ir: também não têm aulas reservadas (a demonstração fica coerente). */
    private final Set<Long> evasores = new java.util.HashSet<>();

    private void popular() {
        evasores.clear();
        Random sorteio = new Random(2026);
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        // --- estrutura da rede
        List<Unidade> unids = new ArrayList<>();
        for (Local l : LOCAIS) {
            unids.add(unidades.save(new Unidade(l.nome(), l.cidade(), l.uf(), l.capacidade())));
        }
        Modalidade musculacao = modalidades.save(new Modalidade("Musculação", "🏋️"));
        Modalidade spinning = modalidades.save(new Modalidade("Spinning", "🚴"));
        Modalidade funcional = modalidades.save(new Modalidade("Funcional", "🔥"));
        Modalidade yoga = modalidades.save(new Modalidade("Yoga", "🧘"));
        Modalidade boxe = modalidades.save(new Modalidade("Boxe", "🥊"));
        Modalidade natacao = modalidades.save(new Modalidade("Natação", "🏊"));

        Plano essencial = new Plano("Essencial", new BigDecimal("99.90"), 1, false, Set.of(musculacao));
        Plano performance = new Plano("Performance", new BigDecimal("149.90"), 1, false,
                orden(musculacao, funcional, spinning, yoga));
        Plano rede = new Plano("Rede", new BigDecimal("199.90"), 6, true,
                orden(musculacao, funcional, spinning, yoga, boxe));
        Plano black = new Plano("Black", new BigDecimal("299.90"), 12, true,
                orden(musculacao, funcional, spinning, yoga, boxe, natacao));
        List<Plano> todosPlanos = planos.saveAll(List.of(essencial, performance, rede, black));

        Map<Long, List<Instrutor>> instrutoresPorUnidade = new HashMap<>();
        String[] instrutoresNomes = { "Renata Moraes", "Caio Bastos", "Larissa Duarte", "Marcos Vidal", "Pedro Lacerda",
                "Júlia Campos", "Thais Brandão", "Rodrigo Pires" };
        for (int i = 0; i < unids.size(); i++) {
            List<Instrutor> lista = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                lista.add(instrutores.save(new Instrutor(instrutoresNomes[(i * 2 + j) % instrutoresNomes.length],
                        unids.get(i))));
            }
            instrutoresPorUnidade.put(unids.get(i).getId(), lista);
        }

        // --- alunos, matrículas, cobranças, avaliações e catraca
        int[] porUnidade = { 52, 78, 62, 54 };
        List<Aluno> todosAlunos = new ArrayList<>();
        Map<Long, Matricula> vigentePorAluno = new HashMap<>();
        int base = 100_000_000;
        int ordem = 0;
        for (int u = 0; u < unids.size(); u++) {
            Unidade unidade = unids.get(u);
            Local local = LOCAIS.get(u);
            for (int i = 0; i < porUnidade[u]; i++, ordem++) {
                String nome = NOMES[sorteio.nextInt(NOMES.length)] + " " + SOBRENOMES[sorteio.nextInt(SOBRENOMES.length)]
                        + " " + SOBRENOMES[sorteio.nextInt(SOBRENOMES.length)];
                String email = semAcento(nome).toLowerCase().replace(' ', '.') + ordem + "@exemplo.com.br";
                LocalDate nascimento = hoje.minusYears(18 + sorteio.nextInt(45)).minusDays(sorteio.nextInt(365));
                Aluno a = alunos.save(new Aluno(nome, Cpf.comBase(base + ordem * 7919 + sorteio.nextInt(7000)), email,
                        nascimento, new Endereco(local.bairros()[sorteio.nextInt(local.bairros().length)],
                                local.cidade(), local.uf()), unidade));
                todosAlunos.add(a);

                Plano plano = sortearPlano(sorteio, essencial, performance, rede, black);
                double s = sorteio.nextDouble();
                // A rede está crescendo: mais matrículas recentes do que antigas (potência 1,4 puxa as datas para perto de hoje).
                int diasDeCasa = 10 + (int) (520 * Math.pow(sorteio.nextDouble(), 1.4));
                LocalDate inicio = hoje.minusDays(diasDeCasa);
                Matricula m = new Matricula(a, plano, inicio);
                boolean inadimplente = false;
                if (s < 0.80) {
                    inadimplente = sorteio.nextDouble() < 0.065;
                } else if (s < 0.86) {
                    m.trancar();
                } else {
                    boolean recente = sorteio.nextDouble() < 0.30;
                    int duracao = Math.max(20, diasDeCasa - 5);
                    LocalDate fim = recente ? hoje.minusDays(1 + sorteio.nextInt(28))
                            : inicio.plusDays(20 + sorteio.nextInt(Math.max(1, duracao - 20)));
                    if (fim.isBefore(inicio.plusDays(15))) {
                        fim = inicio.plusDays(15);
                    }
                    if (fim.isAfter(hoje)) {
                        fim = hoje.minusDays(1);
                    }
                    m.cancelar(fim, MOTIVOS[sorteio.nextInt(MOTIVOS.length)]);
                }
                gerarCobrancas(m, hoje, agora, sorteio, inadimplente);
                matriculas.save(m);
                if (m.vigente()) {
                    vigentePorAluno.put(a.getId(), m);
                }

                if (sorteio.nextDouble() < 0.45) {
                    gerarAvaliacoes(a, hoje, sorteio);
                }
                gerarAcessos(a, m, unids, unidade, agora, sorteio);
            }
        }

        gerarContatos(todosAlunos, vigentePorAluno, agora, sorteio);
        gerarUsuarios(unids, todosAlunos, vigentePorAluno);

        // --- agenda das próximas aulas, com reservas e algumas aulas lotadas
        gerarAgenda(unids, instrutoresPorUnidade, Map.of("Musculação", musculacao, "Spinning", spinning,
                "Funcional", funcional, "Yoga", yoga, "Boxe", boxe), todosAlunos, vigentePorAluno, agora, sorteio);
    }

    /**
     * A equipe já abordou parte de quem parou de ir (sem sucesso ainda) e recuperou alguns alunos nas últimas
     * semanas: é isso que alimenta "retenção" no painel.
     */
    private void gerarContatos(List<Aluno> todos, Map<Long, Matricula> vigentes, LocalDateTime agora, Random sorteio) {
        List<Contato> lote = new ArrayList<>();
        List<Aluno> regulares = new ArrayList<>();
        for (Aluno a : todos) {
            Matricula m = vigentes.get(a.getId());
            if (m == null || !m.ativa()) {
                continue;
            }
            if (evasores.contains(a.getId())) {
                if (sorteio.nextDouble() < 0.35) {
                    lote.add(new Contato(a, sorteio.nextBoolean() ? CanalDeContato.WHATSAPP : CanalDeContato.LIGACAO,
                            sorteio.nextBoolean() ? ResultadoDoContato.PROMETEU_VOLTAR : ResultadoDoContato.SEM_RESPOSTA,
                            null, agora.minusDays(1 + sorteio.nextInt(9)), 55 + sorteio.nextInt(30)));
                }
            } else {
                regulares.add(a);
            }
        }
        String[] notas = { "Estava viajando e voltou na semana seguinte", "Trocou o horário e retomou a rotina",
                "Ofereci aula experimental de Funcional", "Pediu para pausar a cobrança e acabou voltando" };
        for (int i = 0; i < 7 && i < regulares.size(); i++) {
            Aluno a = regulares.get(sorteio.nextInt(regulares.size()));
            lote.add(new Contato(a, CanalDeContato.LIGACAO, ResultadoDoContato.VOLTOU_A_TREINAR,
                    notas[i % notas.length], agora.minusDays(8 + sorteio.nextInt(18)), 60 + sorteio.nextInt(25)));
        }
        contatos.saveAll(lote);
    }

    /**
     * Uma conta de cada perfil para quem avalia o projeto. Todas com a mesma senha de demonstração
     * (dados fictícios). O gerente e a recepção de Vitória só enxergam a unidade deles; o de São Paulo,
     * a dele; o aluno, só o próprio cadastro.
     */
    private void gerarUsuarios(List<Unidade> unids, List<Aluno> todos, Map<Long, Matricula> vigentes) {
        String senha = AutenticacaoController.SENHA_DE_DEMONSTRACAO;
        Unidade vitoria = unids.get(0);
        Unidade saoPaulo = unids.get(1);
        usuarios.save(new Usuario("diretoria@pulso.dev", "Marina Prado", senhas.encode(senha), dev.barboza.pulso.seguranca.Perfil.DIRETORIA, null, null));
        usuarios.save(new Usuario("gerente.vitoria@pulso.dev", "Rogério Tavares", senhas.encode(senha), dev.barboza.pulso.seguranca.Perfil.GERENTE,
                vitoria.getId(), null));
        usuarios.save(new Usuario("gerente.saopaulo@pulso.dev", "Letícia Moura", senhas.encode(senha), dev.barboza.pulso.seguranca.Perfil.GERENTE,
                saoPaulo.getId(), null));
        usuarios.save(new Usuario("recepcao.vitoria@pulso.dev", "Camila Duarte", senhas.encode(senha), dev.barboza.pulso.seguranca.Perfil.RECEPCAO,
                vitoria.getId(), null));
        // o aluno é de Vitória, treina com frequência (fora do grupo que parou de ir) e tem plano que inclui as aulas
        Aluno aluno = todos.stream()
                .filter(a -> a.getUnidade().getId().equals(vitoria.getId()) && !evasores.contains(a.getId()))
                .filter(a -> vigentes.get(a.getId()) != null && vigentes.get(a.getId()).ativa()
                        && vigentes.get(a.getId()).getPlano().getModalidades().size() >= 4) // pode reservar Spinning, Yoga...
                .findFirst().orElseThrow();
        usuarios.save(new Usuario(aluno.getEmail(), aluno.getNome(), senhas.encode(senha), dev.barboza.pulso.seguranca.Perfil.ALUNO, null, aluno.getId()));
    }

    private static final String[] MOTIVOS = { "Mudança de cidade", "Preço", "Falta de tempo", "Lesão",
            "Foi para outra academia", "Não informado" };

    private static Set<Modalidade> orden(Modalidade... ms) {
        return new LinkedHashSet<>(List.of(ms));
    }

    private static Plano sortearPlano(Random r, Plano essencial, Plano performance, Plano rede, Plano black) {
        double x = r.nextDouble();
        return x < 0.35 ? essencial : x < 0.65 ? performance : x < 0.87 ? rede : black;
    }

    private void gerarCobrancas(Matricula m, LocalDate hoje, LocalDateTime agora, Random sorteio, boolean inadimplente) {
        LocalDate fimDoPeriodo = m.getFim() == null ? hoje : m.getFim();
        LocalDate mes = m.getInicio().withDayOfMonth(1);
        LocalDate primeiroMes = hoje.withDayOfMonth(1).minusMonths(5);
        if (mes.isBefore(primeiroMes)) {
            mes = primeiroMes;
        }
        List<Mensalidade> geradas = new ArrayList<>();
        for (; !mes.isAfter(fimDoPeriodo.withDayOfMonth(1)); mes = mes.plusMonths(1)) {
            LocalDate venc = mes.withDayOfMonth(10);
            if (venc.isBefore(m.getInicio())) {
                venc = m.getInicio().plusDays(5);
            }
            geradas.add(m.gerarMensalidade(mes, venc));
        }
        // Inadimplentes: as 1 ou 2 últimas cobranças vencidas ficam sem pagamento.
        int semPagar = inadimplente ? 1 + sorteio.nextInt(2) : 0;
        int vencidas = (int) geradas.stream().filter(x -> x.getVencimento().isBefore(hoje)).count();
        int indice = 0;
        for (Mensalidade x : geradas) {
            boolean vencida = x.getVencimento().isBefore(hoje);
            if (vencida) {
                indice++;
                if (indice > vencidas - semPagar) {
                    continue; // fica em aberto, agora atrasada
                }
                x.pagar(pagamento(x, x.getVencimento().minusDays(sorteio.nextInt(6)).atTime(8 + sorteio.nextInt(10), sorteio.nextInt(60)), sorteio));
            } else if (m.vigente() && sorteio.nextDouble() < 0.5 && !x.getVencimento().isAfter(hoje.plusDays(10))) {
                x.pagar(pagamento(x, agora.minusHours(sorteio.nextInt(70)), sorteio)); // pagou adiantado
            }
        }
    }

    private Pagamento pagamento(Mensalidade x, LocalDateTime quando, Random sorteio) {
        double f = sorteio.nextDouble();
        if (f < 0.55) {
            return new PagamentoPix(x.getValor(), quando,
                    "PULSO" + UUID.nameUUIDFromBytes(("pix" + sorteio.nextLong()).getBytes()).toString()
                            .replace("-", "").substring(0, 20).toUpperCase());
        }
        if (f < 0.85) {
            return new PagamentoCartao(x.getValor(), quando, String.format("%04d", sorteio.nextInt(10000)));
        }
        return new PagamentoBoleto(x.getValor(), quando, "23793.38128 60000.000003 00000.000400 1 "
                + x.getValor().movePointRight(2).toPlainString());
    }

    private void gerarAvaliacoes(Aluno a, LocalDate hoje, Random sorteio) {
        int quantas = 1 + sorteio.nextInt(4);
        BigDecimal altura = new BigDecimal(1.55 + sorteio.nextDouble() * 0.40).setScale(2, java.math.RoundingMode.HALF_UP);
        double peso = 60 + sorteio.nextDouble() * 40;
        double gordura = 18 + sorteio.nextDouble() * 14;
        double cintura = 74 + sorteio.nextDouble() * 24;
        LocalDate data = hoje.minusDays(20 + sorteio.nextInt(30) + (quantas - 1) * 75L);
        for (int i = 0; i < quantas; i++) {
            if (!data.isAfter(hoje)) {
                a.getAvaliacoes().add(new AvaliacaoFisica(a, data,
                        BigDecimal.valueOf(peso).setScale(1, java.math.RoundingMode.HALF_UP), altura,
                        BigDecimal.valueOf(gordura).setScale(1, java.math.RoundingMode.HALF_UP),
                        BigDecimal.valueOf(cintura).setScale(1, java.math.RoundingMode.HALF_UP)));
            }
            data = data.plusDays(70 + sorteio.nextInt(20));
            peso -= 0.4 + sorteio.nextDouble() * 1.6;
            gordura -= 0.3 + sorteio.nextDouble() * 1.2;
            cintura -= 0.5 + sorteio.nextDouble() * 2.0;
        }
    }

    private void gerarAcessos(Aluno a, Matricula m, List<Unidade> todas, Unidade casa, LocalDateTime agora,
            Random sorteio) {
        if (m.getStatus() == dev.barboza.pulso.dominio.StatusMatricula.TRANCADA) {
            return;
        }
        LocalDate limite = m.getFim() == null ? agora.toLocalDate() : m.getFim();
        Perfil perfil;
        double p = sorteio.nextDouble();
        if (m.vigente() && p < 0.22) {
            perfil = Perfil.EVADINDO;
        } else if (p < 0.45) {
            perfil = Perfil.ASSIDUO;
        } else if (p < 0.80) {
            perfil = Perfil.REGULAR;
        } else {
            perfil = Perfil.OCASIONAL;
        }
        if (perfil == Perfil.EVADINDO) {
            evasores.add(a.getId());
        }
        int paraDeIr = perfil == Perfil.EVADINDO ? 8 + sorteio.nextInt(26) : -1; // dias atrás em que parou
        List<Acesso> lote = new ArrayList<>();
        for (int d = 56; d >= 0; d--) {
            LocalDate dia = agora.toLocalDate().minusDays(d);
            if (dia.isBefore(m.getInicio()) || dia.isAfter(limite)) {
                continue;
            }
            if (perfil == Perfil.EVADINDO && d < paraDeIr) {
                continue;
            }
            double chance = switch (perfil) {
                case ASSIDUO -> 0.68;
                case REGULAR -> 0.42;
                case OCASIONAL -> 0.16;
                case EVADINDO -> d > paraDeIr + 14 ? 0.62 : 0.30; // vai caindo antes de sumir
            };
            if (dia.getDayOfWeek().getValue() >= 6) {
                chance *= 0.55;
            }
            if (sorteio.nextDouble() >= chance) {
                continue;
            }
            LocalTime hora = horarioDeTreino(sorteio);
            LocalDateTime entrada = dia.atTime(hora);
            if (entrada.isAfter(agora)) {
                continue;
            }
            Unidade onde = sorteio.nextDouble() < 0.9 || !m.getPlano().isMultiUnidade() ? casa
                    : todas.get(sorteio.nextInt(todas.size()));
            Acesso ac = new Acesso(a, onde, entrada);
            LocalDateTime saida = entrada.plusMinutes(45 + sorteio.nextInt(70));
            if (saida.isBefore(agora)) {
                ac.sair(saida);
            }
            lote.add(ac);
        }
        acessos.saveAll(lote);
    }

    private static LocalTime horarioDeTreino(Random r) {
        double x = r.nextDouble();
        int hora;
        if (x < 0.28) {
            hora = 6 + r.nextInt(3);
        } else if (x < 0.42) {
            hora = 11 + r.nextInt(3);
        } else if (x < 0.88) {
            hora = 17 + r.nextInt(4);
        } else {
            hora = 9 + r.nextInt(2);
        }
        return LocalTime.of(hora, r.nextInt(60));
    }

    private void gerarAgenda(List<Unidade> unids, Map<Long, List<Instrutor>> instrutoresPorUnidade,
            Map<String, Modalidade> mods, List<Aluno> todos, Map<Long, Matricula> vigentes, LocalDateTime agora,
            Random sorteio) {
        record Horario(int hora, int minuto, String modalidade, int vagas, double procura) {
        }
        List<Horario> grade = List.of(new Horario(6, 30, "Spinning", 18, 0.7), new Horario(7, 30, "Funcional", 20, 0.6),
                new Horario(9, 0, "Yoga", 15, 0.5), new Horario(12, 15, "Boxe", 14, 0.6),
                new Horario(18, 0, "Spinning", 20, 1.1), new Horario(19, 0, "Funcional", 22, 1.0),
                new Horario(20, 0, "Yoga", 15, 0.55));

        for (int dia = 0; dia < 5; dia++) {
            LocalDate data = agora.toLocalDate().plusDays(dia);
            for (Unidade u : unids) {
                List<Instrutor> profs = instrutoresPorUnidade.get(u.getId());
                List<Aluno> daUnidade = todos.stream().filter(a -> a.getUnidade().getId().equals(u.getId())).toList();
                for (Horario h : grade) {
                    Modalidade mod = mods.get(h.modalidade());
                    Aula aula = new Aula(mod, u, profs.get(sorteio.nextInt(profs.size())),
                            data.atTime(h.hora(), h.minuto()), h.modalidade().equals("Yoga") ? 60 : 45, h.vagas());
                    int alvo = (int) Math.min(h.vagas() + 5, Math.round(h.vagas() * h.procura() * (0.55 + sorteio.nextDouble() * 0.6)));
                    for (Aluno a : daUnidade) {
                        if (alvo <= 0) {
                            break;
                        }
                        Matricula m = vigentes.get(a.getId());
                        if (m == null || !m.ativa() || evasores.contains(a.getId()) || !m.getPlano().inclui(mod) || sorteio.nextDouble() > 0.35) {
                            continue;
                        }
                        aula.reservar(a, agora.minusHours(sorteio.nextInt(48)));
                        alvo--;
                    }
                    aulas.save(aula);
                }
            }
        }
    }

    private static String semAcento(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
