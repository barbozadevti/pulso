package dev.barboza.pulso.servico;

import java.util.ArrayList;
import java.util.List;

/**
 * Nota de risco de evasão (0 a 100) de um aluno. Função pura, sem banco: recebe os sinais e devolve a nota
 * com os motivos, o que a deixa fácil de testar e de explicar para quem decide.
 *
 * <ul>
 *   <li>até 45 pontos por tempo sem treinar (chega ao máximo com 21 dias);</li>
 *   <li>até 25 por queda de frequência (últimas 2 semanas contra as 2 anteriores);</li>
 *   <li>25 por mensalidade vencida;</li>
 *   <li>10 para quem tem menos de 60 dias de casa e vai pouco (o momento mais frágil).</li>
 * </ul>
 */
public final class PontuacaoDeRisco {

    public enum Nivel { BAIXO, MEDIO, ALTO }

    public record Sinais(Long diasSemTreinar, long visitasRecentes, long visitasAnteriores, long diasDeAtraso,
            long diasDeCasa) {
    }

    public record Resultado(int pontos, Nivel nivel, List<String> fatores) {
    }

    private PontuacaoDeRisco() {
    }

    public static Resultado avaliar(Sinais s) {
        int pontos = 0;
        List<String> fatores = new ArrayList<>();

        long dias = s.diasSemTreinar() == null ? 60 : s.diasSemTreinar();
        if (dias >= 7) {
            pontos += (int) Math.min(45, Math.round(dias * 45.0 / 21));
            fatores.add(s.diasSemTreinar() == null ? "Não treina há mais de 28 dias"
                    : "Sem treinar há " + dias + " dias");
        }

        if (s.visitasAnteriores() >= 4 && s.visitasRecentes() < s.visitasAnteriores() * 0.6) {
            double queda = 1 - (double) s.visitasRecentes() / s.visitasAnteriores();
            pontos += (int) Math.round(25 * Math.min(1, queda / 0.8));
            fatores.add("Frequência caiu " + Math.round(queda * 100) + "% (de " + s.visitasAnteriores() + " para "
                    + s.visitasRecentes() + " visitas)");
        }

        if (s.diasDeAtraso() > 0) {
            pontos += 25;
            fatores.add("Mensalidade vencida há " + s.diasDeAtraso() + " dias");
        }

        if (s.diasDeCasa() < 60 && s.visitasRecentes() + s.visitasAnteriores() < 4) {
            pontos += 10;
            fatores.add("Novo na academia e treinando pouco");
        }

        pontos = Math.min(100, pontos);
        Nivel nivel = pontos >= 55 ? Nivel.ALTO : pontos >= 30 ? Nivel.MEDIO : Nivel.BAIXO;
        return new Resultado(pontos, nivel, fatores);
    }
}
