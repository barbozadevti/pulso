package dev.barboza.pulso.rastro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Guarda, por thread, o SQL que o Hibernate executou durante a requisição (ou durante uma medição).
 * É a base do painel "Por dentro do JPA": mostra quantas consultas cada tela custou.
 */
public final class RastroSql {

    private static final ThreadLocal<List<String>> ATUAL = new ThreadLocal<>();

    private RastroSql() {
    }

    public static void iniciar() {
        ATUAL.set(new ArrayList<>());
    }

    /** Encerra a coleta e devolve o SQL capturado (lista vazia se nada rodou). */
    public static List<String> encerrar() {
        List<String> sqls = ATUAL.get();
        ATUAL.remove();
        return sqls == null ? List.of() : sqls;
    }

    static void registrar(String sql) {
        List<String> sqls = ATUAL.get();
        if (sqls != null) {
            sqls.add(compactar(sql));
        }
    }

    /** Resultado de uma medição: o que a operação devolveu, o SQL que gastou e quanto demorou. */
    public record Medicao<T>(T resultado, List<String> sql, long milissegundos) {
        public int consultas() {
            return sql.size();
        }
    }

    /** Executa a operação medindo SQL e tempo. Dentro de uma requisição, o SQL medido também entra na contagem da requisição. */
    public static <T> Medicao<T> medir(Supplier<T> operacao) {
        List<String> anterior = ATUAL.get();
        iniciar();
        long inicio = System.nanoTime();
        try {
            T resultado = operacao.get();
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            return new Medicao<>(resultado, List.copyOf(ATUAL.get()), ms);
        } finally {
            if (anterior == null) {
                ATUAL.remove();
            } else {
                anterior.addAll(ATUAL.get()); // a requisição inteira continua contando tudo
                ATUAL.set(anterior);
            }
        }
    }

    private static String compactar(String sql) {
        String s = sql.replaceAll("\\s+", " ").trim();
        return s.length() > 600 ? s.substring(0, 600) + "…" : s;
    }
}
