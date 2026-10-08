package dev.barboza.pulso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.barboza.pulso.servico.Cpf;
import dev.barboza.pulso.servico.PontuacaoDeRisco;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Nivel;
import dev.barboza.pulso.servico.PontuacaoDeRisco.Sinais;

/** Regras sem banco nem Spring: rodam em milissegundos. */
class RegrasPuraTest {

    @Test
    void alunoAssiduoNaoTemRisco() {
        var r = PontuacaoDeRisco.avaliar(new Sinais(1L, 8, 9, 0, 300));
        assertThat(r.pontos()).isZero();
        assertThat(r.nivel()).isEqualTo(Nivel.BAIXO);
        assertThat(r.fatores()).isEmpty();
    }

    @Test
    void tresSemanasSemTreinarMaisAtrasoDaRiscoAlto() {
        var r = PontuacaoDeRisco.avaliar(new Sinais(21L, 0, 8, 12, 300));
        assertThat(r.pontos()).isGreaterThanOrEqualTo(60);
        assertThat(r.nivel()).isEqualTo(Nivel.ALTO);
        assertThat(r.fatores()).anyMatch(f -> f.contains("21 dias")).anyMatch(f -> f.contains("vencida há 12"));
    }

    @Test
    void quedaDeFrequenciaPesaMesmoTreinandoRecentemente() {
        var r = PontuacaoDeRisco.avaliar(new Sinais(2L, 2, 10, 0, 300));
        assertThat(r.pontos()).isBetween(15, 30);
        assertThat(r.fatores()).anyMatch(f -> f.contains("Frequência caiu"));
    }

    @Test
    void semNenhumaVisitaEmQuatroSemanasContaComoAusenciaLonga() {
        var r = PontuacaoDeRisco.avaliar(new Sinais(null, 0, 0, 0, 300));
        assertThat(r.pontos()).isEqualTo(40);
        assertThat(r.fatores()).containsExactly("Não treina há mais de 28 dias");
    }

    @Test
    void piorCasoFicaDentroDaEscala() {
        var r = PontuacaoDeRisco.avaliar(new Sinais(null, 0, 12, 90, 300));
        assertThat(r.pontos()).isEqualTo(90).isLessThanOrEqualTo(100);
    }

    @Test
    void cpfValidaDigitosVerificadores() {
        assertThat(Cpf.valido("529.982.247-25")).isTrue();
        assertThat(Cpf.valido("52998224725")).isTrue();
        assertThat(Cpf.valido("529.982.247-24")).isFalse();
        assertThat(Cpf.valido("111.111.111-11")).isFalse();
        assertThat(Cpf.valido("abc")).isFalse();
    }

    @Test
    void cpfsGeradosParaDemonstracaoSaoValidos() {
        for (int i = 0; i < 500; i++) {
            assertThat(Cpf.valido(Cpf.comBase(100_000_000 + i * 7919))).isTrue();
        }
    }
}
