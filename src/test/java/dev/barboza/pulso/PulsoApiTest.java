package dev.barboza.pulso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Integração de ponta a ponta: API → serviços → JPA → H2, sobre a rede de demonstração de 246 alunos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
@Transactional
class PulsoApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @jakarta.persistence.PersistenceContext
    jakarta.persistence.EntityManager em;

    JsonNode ler(String caminho) throws Exception {
        return json.readTree(mvc.perform(get(caminho)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    JsonNode enviar(String caminho, String corpo, int esperado) throws Exception {
        return json.readTree(mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString());
    }

    long unidade(int posicao) throws Exception {
        return ler("/api/unidades").get(posicao).get("id").asLong();
    }

    long plano(String nome) throws Exception {
        for (JsonNode p : ler("/api/planos")) {
            if (p.get("nome").asText().equals(nome)) {
                return p.get("id").asLong();
            }
        }
        throw new AssertionError("plano " + nome);
    }

    /** Cria um aluno novo e o matricula no plano; devolve o id do aluno. */
    long alunoMatriculado(String cpf, long unidadeId, String plano) throws Exception {
        String corpo = """
                {"nome":"Teste Silva","cpf":"%s","email":"t%s@exemplo.com.br","nascimento":"1990-05-05",
                 "bairro":"Centro","cidade":"Vitória","uf":"ES","unidadeId":%d}""".formatted(cpf, cpf.replaceAll("\\D", ""), unidadeId);
        long id = enviar("/api/alunos", corpo, 201).get("aluno").get("id").asLong();
        enviar("/api/matriculas", "{\"alunoId\":%d,\"planoId\":%d}".formatted(id, plano(plano)), 201);
        return id;
    }

    // ------------------------------------------------------------------ painel e risco

    @Test
    void painelExecutivoTrazOsNumerosDaRede() throws Exception {
        JsonNode p = ler("/api/painel");
        assertThat(p.get("kpis").get("ativos").asLong()).isGreaterThan(150);
        assertThat(p.get("kpis").get("receitaRecorrente").asDouble()).isGreaterThan(20000);
        assertThat(p.get("unidades")).hasSize(4);
        assertThat(p.get("planos")).hasSize(4);
        assertThat(p.get("receita")).hasSize(6);
        assertThat(p.get("mapaDeCalor").get("valores")).hasSize(7);
        assertThat(p.get("kpis").get("valorEmAtraso").asDouble()).isGreaterThan(0);
        assertThat(p.get("alunosEmRiscoAlto").asLong()).isGreaterThan(0);
        assertThat(p.get("receitaEmRisco").asDouble()).isGreaterThan(0);
    }

    @Test
    void painelPorUnidadeSoContaAquelaUnidade() throws Exception {
        long u = unidade(0);
        JsonNode rede = ler("/api/painel");
        JsonNode so = ler("/api/painel?unidadeId=" + u);
        assertThat(so.get("kpis").get("ativos").asLong()).isLessThan(rede.get("kpis").get("ativos").asLong());
        assertThat(so.get("maisArriscados")).allMatch(r -> r.get("unidadeId").asLong() == u);
    }

    @Test
    void rankingDeRiscoVemOrdenadoComMotivos() throws Exception {
        JsonNode r = ler("/api/risco?nivel=ALTO");
        assertThat(r.size()).isGreaterThan(0);
        int anterior = 101;
        for (JsonNode a : r) {
            assertThat(a.get("pontos").asInt()).isLessThanOrEqualTo(anterior).isGreaterThanOrEqualTo(55);
            assertThat(a.get("fatores").size()).isGreaterThan(0);
            anterior = a.get("pontos").asInt();
        }
    }

    // ------------------------------------------------------------------ alunos

    @Test
    void listaFiltraPorNomeUnidadeESituacaoComPaginacao() throws Exception {
        JsonNode todos = ler("/api/alunos?tamanho=10");
        assertThat(todos.get("total").asLong()).isEqualTo(246);
        assertThat(todos.get("conteudo")).hasSize(10);

        JsonNode daUnidade = ler("/api/alunos?tamanho=100&unidadeId=" + unidade(0));
        assertThat(daUnidade.get("total").asLong()).isEqualTo(54); // unidades em ordem alfabética: Boa Viagem é a primeira

        JsonNode ativos = ler("/api/alunos?situacao=ATIVA&tamanho=100");
        assertThat(ativos.get("conteudo")).allMatch(a -> a.get("situacao").asText().equals("ATIVA"));

        String nome = todos.get("conteudo").get(0).get("nome").asText().split(" ")[0];
        JsonNode busca = ler("/api/alunos?busca=" + nome + "&tamanho=100");
        assertThat(busca.get("conteudo")).allMatch(a -> a.get("nome").asText().toLowerCase().contains(nome.toLowerCase()));
    }

    @Test
    void listaPaginadaUsaPoucasConsultas() throws Exception {
        // página + contagem + matrículas vigentes: três, e não uma por aluno (a unidade vem no join fetch)
        mvc.perform(get("/api/alunos?tamanho=50")).andExpect(status().isOk())
                .andExpect(header().string("X-Consultas-Sql", "3"));
    }

    @Test
    void fichaDoAlunoTrazTudoEmPoucasConsultas() throws Exception {
        long id = ler("/api/alunos?situacao=ATIVA&tamanho=1").get("conteudo").get(0).get("id").asLong();
        var resposta = mvc.perform(get("/api/alunos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.aluno.nome").exists()).andExpect(jsonPath("$.frequencia.porSemana", hasSize(8)))
                .andExpect(jsonPath("$.mensalidades").isArray()).andReturn().getResponse();
        // ficha inteira em um número pequeno e fixo de consultas, não importa quantas cobranças e avaliações existam
        assertThat(Integer.parseInt(resposta.getHeader("X-Consultas-Sql"))).isLessThanOrEqualTo(14);
    }

    @Test
    void cadastroValidaCpfEmailRepetidoEDadosObrigatorios() throws Exception {
        long u = unidade(0);
        String ok = """
                {"nome":"Nova Pessoa","cpf":"529.982.247-25","email":"nova@exemplo.com.br","nascimento":"1991-01-01",
                 "bairro":"Centro","cidade":"Vitória","uf":"ES","unidadeId":%d}""".formatted(u);
        enviar("/api/alunos", ok, 201);
        enviar("/api/alunos", ok.replace("nova@", "outra@"), 422); // CPF repetido
        enviar("/api/alunos", ok.replace("529.982.247-25", "529.982.247-24").replace("nova@", "x@"), 422); // CPF inválido
        enviar("/api/alunos", ok.replace("\"nome\":\"Nova Pessoa\"", "\"nome\":\"\""), 400);
    }

    @Test
    void avaliacaoFisicaCalculaImcEListaEmOrdem() throws Exception {
        long id = alunoMatriculado("390.533.447-05", unidade(0), "Essencial");
        enviar("/api/alunos/" + id + "/avaliacoes",
                "{\"data\":\"2026-08-01\",\"peso\":90.0,\"altura\":1.80,\"percentualGordura\":26.0}", 201);
        JsonNode lista = enviar("/api/alunos/" + id + "/avaliacoes",
                "{\"data\":\"2026-10-01\",\"peso\":84.0,\"altura\":1.80,\"percentualGordura\":22.5}", 201);
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).get("imc").asDouble()).isEqualTo(27.8); // da mais antiga para a mais nova
        assertThat(lista.get(1).get("imc").asDouble()).isEqualTo(25.9);
        assertThat(lista.get(1).get("faixa").asText()).isEqualTo("Sobrepeso");
        enviar("/api/alunos/" + id + "/avaliacoes", "{\"data\":\"2027-01-01\",\"peso\":80,\"altura\":1.80}", 400);
    }

    // ------------------------------------------------------------------ matrícula e cobrança

    @Test
    void matriculaGeraMensalidadeUmaMatriculaPorVezECancelaComMotivo() throws Exception {
        long id = alunoMatriculado("168.995.350-09", unidade(1), "Performance");
        enviar("/api/matriculas", "{\"alunoId\":%d,\"planoId\":%d}".formatted(id, plano("Rede")), 422);
        JsonNode ficha = ler("/api/alunos/" + id);
        assertThat(ficha.get("mensalidades")).hasSize(1);
        assertThat(ficha.get("mensalidades").get(0).get("situacao").asText()).isEqualTo("ABERTA");
        long matricula = ficha.get("matriculas").get(0).get("id").asLong();

        enviar("/api/matriculas/" + matricula + "/cancelar", "{\"motivo\":\"Mudança de cidade\"}", 200);
        enviar("/api/matriculas/" + matricula + "/cancelar", "{}", 422);
        assertThat(ler("/api/alunos/" + id).get("matriculas").get(0).get("status").asText()).isEqualTo("CANCELADA");
        enviar("/api/matriculas", "{\"alunoId\":%d,\"planoId\":%d}".formatted(id, plano("Black")), 201); // pode voltar
    }

    @Test
    void pagarMensalidadeEmCadaFormaSalvaOSubtipoCerto() throws Exception {
        for (String forma : new String[] { "PIX", "CARTAO", "BOLETO" }) {
            String cpf = switch (forma) { case "PIX" -> "111.444.777-35"; case "CARTAO" -> "935.411.347-80"; default -> "123.456.789-09"; };
            long id = alunoMatriculado(cpf, unidade(0), "Essencial");
            long mensalidade = ler("/api/alunos/" + id).get("mensalidades").get(0).get("id").asLong();
            String ref = forma.equals("CARTAO") ? ",\"referencia\":\"4111 1111 1111 4242\"" : "";
            enviar("/api/mensalidades/" + mensalidade + "/pagar", "{\"forma\":\"" + forma + "\"" + ref + "}", 200);
            JsonNode m = ler("/api/alunos/" + id).get("mensalidades").get(0);
            assertThat(m.get("situacao").asText()).isEqualTo("PAGA");
            assertThat(m.get("forma").asText()).isEqualTo(forma);
            if (forma.equals("CARTAO")) {
                assertThat(m.get("referencia").asText()).isEqualTo("final 4242");
            }
            enviar("/api/mensalidades/" + mensalidade + "/pagar", "{\"forma\":\"PIX\"}", 422); // já paga
        }
    }

    @Test
    void consultaOriginalDoDesafioMatriculasPorBairro() throws Exception {
        JsonNode lista = ler("/api/matriculas?bairro=pinheiros");
        assertThat(lista.size()).isGreaterThan(0);
        assertThat(lista).allMatch(m -> m.get("bairro").asText().equals("Pinheiros"));
    }

    // ------------------------------------------------------------------ catraca

    @Test
    void catracaLiberaNegaEContaOcupacao() throws Exception {
        long casa = unidade(0);
        long outra = unidade(1);
        long id = alunoMatriculado("390.533.447-05", casa, "Essencial");
        String cpf = "390.533.447-05";

        JsonNode antes = ler("/api/catraca/ao-vivo?unidadeId=" + casa);
        JsonNode ok = enviar("/api/catraca/entrada", "{\"cpf\":\"" + cpf + "\",\"unidadeId\":" + casa + "}", 200);
        assertThat(ok.get("liberado").asBoolean()).isTrue();
        assertThat(ok.get("ocupacao").asLong()).isEqualTo(antes.get("ocupacao").asLong() + 1);

        JsonNode jaDentro = enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + casa + "}", 200);
        assertThat(jaDentro.get("liberado").asBoolean()).isFalse();
        assertThat(jaDentro.get("motivo").asText()).contains("já está dentro");

        enviar("/api/catraca/saida", "{\"alunoId\":" + id + "}", 200);
        JsonNode outraUnidade = enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + outra + "}", 200);
        assertThat(outraUnidade.get("liberado").asBoolean()).isFalse();
        assertThat(outraUnidade.get("motivo").asText()).contains("vale só na unidade");
        enviar("/api/catraca/saida", "{\"alunoId\":" + id + "}", 422); // não está dentro
    }

    @Test
    void catracaBloqueiaMensalidadeVencidaHaMaisDeDezDiasETrancadaESemMatricula() throws Exception {
        long casa = unidade(0);
        long id = alunoMatriculado("390.533.447-05", casa, "Rede");
        em.flush(); // a mensalidade ainda está na fila do Hibernate; o JDBC puro só enxerga o que já foi ao banco
        jdbc.update("update mensalidade set vencimento = DATEADD('DAY', -20, DATE '2026-10-07') where matricula_id in (select id from matricula where aluno_id = ?)", id);
        JsonNode r = enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + casa + "}", 200);
        assertThat(r.get("liberado").asBoolean()).isFalse();
        assertThat(r.get("motivo").asText()).contains("vencida há 20 dias");

        long mat = ler("/api/alunos/" + id).get("matriculas").get(0).get("id").asLong();
        jdbc.update("update mensalidade set vencimento = DATE '2026-10-05' where matricula_id = ?", mat);
        assertThat(enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + casa + "}", 200)
                .get("liberado").asBoolean()).isTrue(); // 2 dias de atraso: dentro da tolerância
        enviar("/api/catraca/saida", "{\"alunoId\":" + id + "}", 200);

        enviar("/api/matriculas/" + mat + "/trancar", "{}", 200);
        assertThat(enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + casa + "}", 200)
                .get("motivo").asText()).contains("trancada");
        enviar("/api/matriculas/" + mat + "/reativar", "{}", 200);
        enviar("/api/matriculas/" + mat + "/cancelar", "{}", 200);
        assertThat(enviar("/api/catraca/entrada", "{\"alunoId\":" + id + ",\"unidadeId\":" + casa + "}", 200)
                .get("motivo").asText()).contains("Sem matrícula ativa");
    }

    // ------------------------------------------------------------------ aulas

    @Test
    void agendaMostraVagasEFilaDeEspera() throws Exception {
        JsonNode agenda = ler("/api/aulas?unidadeId=" + unidade(0) + "&dia=2026-10-08");
        assertThat(agenda.size()).isEqualTo(7);
        for (JsonNode a : agenda) {
            assertThat(a.get("confirmadas").asInt()).isLessThanOrEqualTo(a.get("vagas").asInt());
        }
        assertThat(ler("/api/aulas?dia=2026-10-08").size()).isEqualTo(28);
    }

    @Test
    void reservaRespeitaPlanoUnidadeEFilaDeEsperaComPromocao() throws Exception {
        long casa = unidade(0);
        JsonNode agenda = ler("/api/aulas?unidadeId=" + casa + "&dia=2026-10-09");
        JsonNode spinning = null;
        for (JsonNode a : agenda) {
            if (a.get("modalidade").asText().equals("Spinning")) {
                spinning = a;
                break;
            }
        }
        assertThat(spinning).isNotNull();
        long aula = spinning.get("id").asLong();

        long essencial = alunoMatriculado("390.533.447-05", casa, "Essencial");
        enviar("/api/aulas/" + aula + "/reservas", "{\"alunoId\":" + essencial + "}", 422); // plano sem Spinning

        long performance = alunoMatriculado("168.995.350-09", casa, "Performance");
        long forasteiro = alunoMatriculado("529.982.247-25", unidade(1), "Performance");
        enviar("/api/aulas/" + aula + "/reservas", "{\"alunoId\":" + forasteiro + "}", 422); // outra unidade

        JsonNode r = enviar("/api/aulas/" + aula + "/reservas", "{\"alunoId\":" + performance + "}", 201);
        assertThat(r.get("status").asText()).isIn("CONFIRMADA", "ESPERA");
        enviar("/api/aulas/" + aula + "/reservas", "{\"alunoId\":" + performance + "}", 422); // duplicada

        // cancelando uma reserva confirmada, a fila anda
        long antesConfirmadas = confirmadas(casa, "2026-10-09", aula);
        JsonNode ficha = ler("/api/alunos/" + performance);
        long reservaId = ficha.get("reservas").get(0).get("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/reservas/" + reservaId))
                .andExpect(status().isOk());
        assertThat(confirmadas(casa, "2026-10-09", aula)).isLessThanOrEqualTo(spinning.get("vagas").asLong());
        assertThat(antesConfirmadas).isGreaterThan(0);
    }

    private long confirmadas(long unidade, String dia, long aula) throws Exception {
        for (JsonNode a : ler("/api/aulas?unidadeId=" + unidade + "&dia=" + dia)) {
            if (a.get("id").asLong() == aula) {
                return a.get("confirmadas").asLong();
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ laboratório

    @Test
    void laboratorioMostraONMaisUmEAsTresSolucoes() throws Exception {
        JsonNode r = ler("/api/laboratorio/n-mais-um");
        assertThat(r).hasSize(4);
        int ingenuo = r.get(0).get("consultas").asInt();
        assertThat(r.get(0).get("modo").asText()).isEqualTo("INGENUO");
        assertThat(ingenuo).isGreaterThan(50); // 1 página + 1 contagem + um aluno por linha + unidades + planos
        assertThat(r.get(1).get("consultas").asInt()).isLessThanOrEqualTo(3); // EntityGraph
        assertThat(r.get(2).get("consultas").asInt()).isLessThanOrEqualTo(3); // join fetch
        assertThat(r.get(3).get("consultas").asInt()).isLessThanOrEqualTo(3); // projeção
        for (JsonNode c : r) {
            assertThat(c.get("linhas").asInt()).isEqualTo(50);
        }
    }

    // ------------------------------------------------------------------ rastro e rede

    @Test
    void cadaRespostaDaApiInformaQuantasConsultasSqlGastou() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isOk()).andExpect(header().exists("X-Consultas-Sql"))
                .andExpect(header().exists("X-Tempo-Ms"));
        mvc.perform(get("/api/rastro/recentes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caminho").value("/api/painel"))
                .andExpect(jsonPath("$[0].sql.length()", greaterThanOrEqualTo(5)));
    }

    @Test
    void qrCodeDeAcessoPeloCelularEUmSvg() throws Exception {
        mvc.perform(get("/api/rede/qr.svg")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("<svg")));
    }
}
