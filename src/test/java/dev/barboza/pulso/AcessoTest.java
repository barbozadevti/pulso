package dev.barboza.pulso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.api.AutenticacaoController;
import dev.barboza.pulso.seguranca.Perfil;
import dev.barboza.pulso.seguranca.Usuario;
import dev.barboza.pulso.seguranca.UsuarioLogado;
import dev.barboza.pulso.seguranca.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Login, bloqueio por tentativas, CSRF e o isolamento de dados entre perfis e unidades. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioFixo.class)
@Transactional
class AcessoTest {

    static final String SENHA = AutenticacaoController.SENHA_DE_DEMONSTRACAO;

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder senhas;

    UsuarioLogado conta(String login) {
        return usuarios.findByLogin(login).orElseThrow().comoLogado();
    }

    RequestPostProcessor gerenteVitoria() { return Contas.como(conta("gerente.vitoria@pulso.dev")); }
    RequestPostProcessor recepcaoVitoria() { return Contas.como(conta("recepcao.vitoria@pulso.dev")); }
    RequestPostProcessor diretoria() { return Contas.como(conta("diretoria@pulso.dev")); }
    RequestPostProcessor aluna() {
        return Contas.como(usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.ALUNO).findFirst()
                .orElseThrow().comoLogado());
    }

    JsonNode corpo(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    long idDaUnidade(String parteDoNome) throws Exception {
        for (JsonNode u : corpo(mvc.perform(get("/api/unidades").with(diretoria())))) {
            if (u.get("nome").asText().contains(parteDoNome)) {
                return u.get("id").asLong();
            }
        }
        throw new AssertionError(parteDoNome);
    }

    // ------------------------------------------------------------------ login

    @Test
    void semLoginNaoEntraEOsitePublicoCarrega() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/alunos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
    }

    @Test
    void loginCriaSessaoEEuRetornaQuemEstaLogado() throws Exception {
        var r = mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"Gerente.Vitoria@pulso.dev\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessao = (MockHttpSession) r.getRequest().getSession(false);
        assertThat(sessao).isNotNull();
        JsonNode eu = json.readTree(mvc.perform(get("/api/auth/eu").session(sessao)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(eu.get("perfil").asText()).isEqualTo("GERENTE");
        assertThat(eu.get("nome").asText()).isEqualTo("Rogério Tavares");
        mvc.perform(get("/api/painel").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/sair").with(csrf()).session(sessao)).andExpect(status().isNoContent());
    }

    @Test
    void senhaErradaEUsuarioInexistenteRespondemIgual() throws Exception {
        String a = mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"diretoria@pulso.dev\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String b = mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"ninguem@pulso.dev\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(a).get("detail").asText()).isEqualTo(json.readTree(b).get("detail").asText());
    }

    @Test
    void cincoSenhasErradasBloqueiamAteComASenhaCerta() throws Exception {
        usuarios.save(new Usuario("bloqueio@pulso.dev", "Teste", senhas.encode(SENHA), Perfil.RECEPCAO, 1L, null));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"login\":\"bloqueio@pulso.dev\",\"senha\":\"errada" + i + "\"}"))
                    .andExpect(status().is(i < 4 ? 401 : 429));
        }
        mvc.perform(post("/api/auth/entrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"bloqueio@pulso.dev\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void postSemTokenCsrfEBarrado() throws Exception {
        mvc.perform(post("/api/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"diretoria@pulso.dev\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/catraca/saida").with(diretoria()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"alunoId\":1}")).andExpect(status().isForbidden());
    }

    @Test
    void contasDeDemonstracaoSaoListadasParaQuemAvalia() throws Exception {
        JsonNode contas = corpo(mvc.perform(get("/api/auth/demo")));
        assertThat(contas.size()).isEqualTo(5);
        assertThat(contas.get(0).get("perfil").asText()).isEqualTo("DIRETORIA");
        assertThat(contas.get(0).get("senha").asText()).isEqualTo(SENHA);
    }

    // ------------------------------------------------------------------ gerente: só a unidade dele

    @Test
    void gerenteVeSoAPropriaUnidadeNoPainelNaListaENoRisco() throws Exception {
        long vitoria = idDaUnidade("Praia do Canto");
        long saoPaulo = idDaUnidade("Pinheiros");

        JsonNode painel = corpo(mvc.perform(get("/api/painel").with(gerenteVitoria())).andExpect(status().isOk()));
        assertThat(painel.get("unidades")).hasSize(1);
        assertThat(painel.get("unidades").get(0).get("id").asLong()).isEqualTo(vitoria);
        assertThat(painel.get("kpis").get("ativos").asLong())
                .isEqualTo(painel.get("unidades").get(0).get("ativos").asLong());
        long redeInteira = corpo(mvc.perform(get("/api/painel").with(diretoria()))).get("kpis").get("ativos").asLong();
        assertThat(painel.get("kpis").get("ativos").asLong()).isLessThan(redeInteira);

        mvc.perform(get("/api/painel?unidadeId=" + saoPaulo).with(gerenteVitoria())).andExpect(status().isNotFound());

        JsonNode lista = corpo(mvc.perform(get("/api/alunos?tamanho=100").with(gerenteVitoria())));
        assertThat(lista.get("total").asLong()).isEqualTo(52);
        assertThat(lista.get("conteudo")).allMatch(a -> a.get("unidadeId").asLong() == vitoria);

        assertThat(corpo(mvc.perform(get("/api/risco").with(gerenteVitoria()))))
                .isNotEmpty().allMatch(r -> r.get("unidadeId").asLong() == vitoria);
        assertThat(corpo(mvc.perform(get("/api/unidades").with(gerenteVitoria())))).hasSize(1);
        assertThat(corpo(mvc.perform(get("/api/unidades").with(diretoria()))).size()).isEqualTo(4);
    }

    @Test
    void gerenteNaoAlcancaAlunoMatriculaNemCobrancaDeOutraUnidade() throws Exception {
        long saoPaulo = idDaUnidade("Pinheiros");
        long alunoDeFora = corpo(mvc.perform(get("/api/alunos?tamanho=1&unidadeId=" + saoPaulo).with(diretoria())))
                .get("conteudo").get(0).get("id").asLong();
        JsonNode ficha = corpo(mvc.perform(get("/api/alunos/" + alunoDeFora).with(diretoria())));
        long matricula = ficha.get("matriculas").get(0).get("id").asLong();

        mvc.perform(get("/api/alunos/" + alunoDeFora).with(gerenteVitoria())).andExpect(status().isNotFound());
        mvc.perform(post("/api/alunos/" + alunoDeFora + "/contatos").with(gerenteVitoria()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"WHATSAPP\",\"resultado\":\"SEM_RESPOSTA\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/matriculas/" + matricula + "/cancelar").with(gerenteVitoria()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/catraca/entrada").with(gerenteVitoria()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"alunoId\":" + alunoDeFora + ",\"unidadeId\":" + saoPaulo + "}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/alunos").with(gerenteVitoria()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Fora","cpf":"529.982.247-25","email":"fora@exemplo.com.br","nascimento":"1990-01-01",
                         "bairro":"X","cidade":"Y","uf":"SP","unidadeId":%d}""".formatted(saoPaulo)))
                .andExpect(status().isNotFound());
        // a gestão da própria unidade, ao contrário, funciona
        long vitoria = idDaUnidade("Praia do Canto");
        long meu = corpo(mvc.perform(get("/api/alunos?tamanho=1&unidadeId=" + vitoria).with(gerenteVitoria())))
                .get("conteudo").get(0).get("id").asLong();
        mvc.perform(post("/api/alunos/" + meu + "/contatos").with(gerenteVitoria()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"canal\":\"LIGACAO\",\"resultado\":\"SEM_RESPOSTA\"}"))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ recepção: balcão, sem gestão

    @Test
    void recepcaoOperaOBalcaoMasNaoVePainelRiscoNemRetencao() throws Exception {
        mvc.perform(get("/api/painel").with(recepcaoVitoria())).andExpect(status().isForbidden());
        mvc.perform(get("/api/risco").with(recepcaoVitoria())).andExpect(status().isForbidden());
        mvc.perform(get("/api/risco.csv").with(recepcaoVitoria())).andExpect(status().isForbidden());
        mvc.perform(get("/api/laboratorio/n-mais-um").with(recepcaoVitoria())).andExpect(status().isForbidden());
        mvc.perform(get("/api/rastro/recentes").with(recepcaoVitoria())).andExpect(status().isForbidden());
        mvc.perform(post("/api/laboratorio/disputa").with(recepcaoVitoria()).with(csrf())).andExpect(status().isForbidden());

        long vitoria = idDaUnidade("Praia do Canto");
        mvc.perform(get("/api/alunos?unidadeId=" + vitoria).with(recepcaoVitoria())).andExpect(status().isOk());
        mvc.perform(get("/api/catraca/ao-vivo?unidadeId=" + vitoria).with(recepcaoVitoria())).andExpect(status().isOk());
        mvc.perform(get("/api/mensalidades/atrasadas").with(recepcaoVitoria())).andExpect(status().isOk());

        long alunoId = corpo(mvc.perform(get("/api/alunos?tamanho=1&situacao=ATIVA").with(recepcaoVitoria())))
                .get("conteudo").get(0).get("id").asLong();
        mvc.perform(post("/api/alunos/" + alunoId + "/contatos").with(recepcaoVitoria()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"canal\":\"LIGACAO\",\"resultado\":\"SEM_RESPOSTA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/matriculas/1/cancelar").with(recepcaoVitoria()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        // a ficha que a recepção recebe não traz a nota de risco nem as anotações de retenção
        JsonNode ficha = corpo(mvc.perform(get("/api/alunos/" + alunoId).with(recepcaoVitoria())));
        assertThat(ficha.get("risco").isNull()).isTrue();
        assertThat(ficha.get("contatos")).isEmpty();
    }

    @Test
    void listaDaRecepcaoSoTemAlunosDaUnidadeELaBuscaPorCpfNaoVazaOutras() throws Exception {
        long vitoria = idDaUnidade("Praia do Canto");
        JsonNode lista = corpo(mvc.perform(get("/api/alunos?tamanho=100").with(recepcaoVitoria())));
        assertThat(lista.get("conteudo")).allMatch(a -> a.get("unidadeId").asLong() == vitoria);
        // pedir outra unidade explicitamente não funciona
        mvc.perform(get("/api/alunos?unidadeId=" + idDaUnidade("Pinheiros")).with(recepcaoVitoria()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ aluno: só o próprio

    @Test
    void alunoVeSoOPropriaFichaSemRiscoNemRetencaoESoReservaEmNomeProprio() throws Exception {
        UsuarioLogado eu = usuarios.findAll().stream().filter(u -> u.getPerfil() == Perfil.ALUNO).findFirst()
                .orElseThrow().comoLogado();
        mvc.perform(get("/api/alunos").with(aluna())).andExpect(status().isForbidden());
        mvc.perform(get("/api/alunos/" + (eu.alunoId() + 1)).with(aluna())).andExpect(status().isNotFound());

        JsonNode ficha = corpo(mvc.perform(get("/api/alunos/" + eu.alunoId()).with(aluna())).andExpect(status().isOk()));
        assertThat(ficha.get("aluno").get("id").asLong()).isEqualTo(eu.alunoId());
        assertThat(ficha.get("risco").isNull()).isTrue();
        assertThat(ficha.get("contatos")).isEmpty();

        for (String proibido : new String[] { "/api/painel", "/api/risco", "/api/risco.csv", "/api/laboratorio/n-mais-um",
                "/api/rastro/recentes", "/api/mensalidades/atrasadas", "/api/matriculas" }) {
            mvc.perform(get(proibido).with(aluna())).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/catraca/entrada").with(aluna()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"alunoId\":" + eu.alunoId() + ",\"unidadeId\":1}")).andExpect(status().isForbidden());

        // a agenda e a reserva funcionam, sempre em nome de quem está logado
        JsonNode agenda = corpo(mvc.perform(get("/api/aulas?dia=2026-10-09").with(aluna())).andExpect(status().isOk()));
        assertThat(agenda).isNotEmpty();
        long achada = 0;
        for (JsonNode a : agenda) {
            if (a.get("modalidade").asText().equals("Spinning") && a.get("vagas").asInt() > a.get("confirmadas").asInt()) {
                achada = a.get("id").asLong();
                break;
            }
        }
        final long spinning = achada;
        assertThat(spinning).isPositive();
        long outraPessoa = eu.alunoId() + 1;
        mvc.perform(post("/api/aulas/" + spinning + "/reservas").with(aluna()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"alunoId\":" + outraPessoa + "}"))
                .andExpect(status().isCreated());
        JsonNode futuras = corpo(mvc.perform(get("/api/alunos/" + eu.alunoId()).with(aluna()))).get("reservas");
        assertThat(futuras).anyMatch(r -> r.get("aulaId").asLong() == spinning); // a reserva ficou com a aluna logada

        // e ela só cancela reserva própria
        long dela = futuras.get(0).get("id").asLong();
        mvc.perform(delete("/api/reservas/" + dela).with(aluna()).with(csrf())).andExpect(status().isOk());
    }
}
