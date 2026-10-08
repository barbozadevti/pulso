package dev.barboza.pulso.rastro;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Mede cada chamada /api: cabeçalhos X-Consultas-Sql e X-Tempo-Ms, e alimenta o histórico. */
@Component
public class RastroFilter extends OncePerRequestFilter {

    private final HistoricoSql historico;

    public RastroFilter(HistoricoSql historico) {
        this.historico = historico;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !caminho.startsWith("/api/") || caminho.startsWith("/api/rastro");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper resposta = new ContentCachingResponseWrapper(res);
        long inicio = System.nanoTime();
        RastroSql.iniciar();
        try {
            chain.doFilter(req, resposta);
        } finally {
            List<String> sql = RastroSql.encerrar();
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            resposta.setHeader("X-Consultas-Sql", String.valueOf(sql.size()));
            resposta.setHeader("X-Tempo-Ms", String.valueOf(ms));
            resposta.setHeader("Access-Control-Expose-Headers", "X-Consultas-Sql, X-Tempo-Ms, X-Requisicao");
            historico.adicionar(req.getMethod(), req.getRequestURI(), resposta.getStatus(), ms, sql);
            resposta.setHeader("X-Requisicao", String.valueOf(historico.recentes(1).get(0).id()));
            resposta.copyBodyToResponse();
        }
    }
}
