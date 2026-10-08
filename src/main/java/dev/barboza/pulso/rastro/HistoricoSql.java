package dev.barboza.pulso.rastro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/** Últimas requisições da API com o SQL de cada uma (em memória, só as 60 mais recentes). */
@Component
public class HistoricoSql {

    public record Registro(long id, Instant quando, String metodo, String caminho, int status, long milissegundos,
            List<String> sql) {
    }

    private static final int LIMITE = 60;

    private final Deque<Registro> registros = new ConcurrentLinkedDeque<>();
    private long proximo = 0;

    synchronized void adicionar(String metodo, String caminho, int status, long ms, List<String> sql) {
        registros.addFirst(new Registro(++proximo, Instant.now(), metodo, caminho, status, ms, sql));
        while (registros.size() > LIMITE) {
            registros.pollLast();
        }
    }

    public List<Registro> recentes(int quantos) {
        List<Registro> copia = new ArrayList<>(registros);
        return Collections.unmodifiableList(copia.subList(0, Math.min(quantos, copia.size())));
    }
}
