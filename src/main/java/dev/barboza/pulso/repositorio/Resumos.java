package dev.barboza.pulso.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.barboza.pulso.dominio.ResultadoDoContato;

/** Projeções para DTO: o Hibernate monta o record direto do SELECT, sem carregar entidades. */
public final class Resumos {

    private Resumos() {
    }

    public record Frequencia(Long alunoId, LocalDateTime ultimaVisita, long ultimas2Semanas, long duasSemanasAntes) {
    }

    public record UltimoContato(Long alunoId, LocalDateTime feitoEm, ResultadoDoContato resultado) {
    }

    public record Atraso(Long alunoId, LocalDate vencimentoMaisAntigo) {
    }

    public record PorUnidade(Long unidadeId, long quantidade, BigDecimal valor) {
    }

    public record Contagem(Long unidadeId, long quantidade) {
    }

    public record PorPlano(String plano, long quantidade, BigDecimal valor) {
    }

    public record PorMes(LocalDate competencia, BigDecimal faturado, BigDecimal pendente) {
    }

    public record LinhaDaMatricula(Long matriculaId, String aluno, String unidade, String plano) {
    }
}
