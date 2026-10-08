package dev.barboza.pulso.servico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Modelos de leitura: o que as telas recebem. Montados dentro da transação, então nunca expõem proxies lazy. */
public final class Visoes {

    private Visoes() {
    }

    public record ResumoDoAluno(Long id, String nome, String cpf, String email, String bairro, String cidade,
            String uf, Long unidadeId, String unidade, String situacao, String plano, int idade) {
    }

    public record MatriculaVisao(Long id, Long planoId, String plano, BigDecimal valor, LocalDate inicio,
            LocalDate fim, String status, String motivoCancelamento) {
    }

    public record MensalidadeVisao(Long id, LocalDate competencia, BigDecimal valor, LocalDate vencimento,
            String situacao, long diasDeAtraso, String forma, String referencia, LocalDateTime pagoEm) {
    }

    public record AvaliacaoVisao(Long id, LocalDate data, BigDecimal peso, BigDecimal altura, BigDecimal imc,
            String faixa, BigDecimal percentualGordura, BigDecimal cinturaCm) {
    }

    /** {@code porSemana}: visitas em cada uma das últimas 8 semanas, da mais antiga para a mais recente. */
    public record FrequenciaVisao(long visitas30Dias, Long diasSemTreinar, LocalDateTime ultimaVisita,
            List<Integer> porSemana) {
    }

    public record RiscoVisao(int pontos, String nivel, List<String> fatores) {
    }

    public record ReservaVisao(Long id, Long aulaId, String modalidade, String icone, String unidade,
            LocalDateTime inicio, String status) {
    }

    public record ContatoVisao(Long id, String canal, String resultado, String observacao, LocalDateTime feitoEm,
            int riscoNaEpoca) {
    }

    public record Ficha(ResumoDoAluno aluno, LocalDate nascimento, List<MatriculaVisao> matriculas,
            List<MensalidadeVisao> mensalidades, List<AvaliacaoVisao> avaliacoes, FrequenciaVisao frequencia,
            RiscoVisao risco, List<ReservaVisao> reservas, List<ContatoVisao> contatos) {
    }
}
