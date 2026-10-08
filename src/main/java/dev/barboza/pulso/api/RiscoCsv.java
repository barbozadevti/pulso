package dev.barboza.pulso.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.barboza.pulso.servico.RiscoService.AlunoEmRisco;

/** CSV para o gerente abrir no Excel: separador ";", vírgula decimal e BOM UTF-8 para os acentos. */
final class RiscoCsv {

    private RiscoCsv() {
    }

    static byte[] gerar(List<AlunoEmRisco> alunos) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Aluno;Unidade;Plano;Mensalidade (R$);Nota;Nivel;Dias sem treinar;Ultimo contato (dias);Motivos\r\n");
        for (AlunoEmRisco a : alunos) {
            sb.append(celula(a.nome())).append(';').append(celula(a.unidade())).append(';').append(celula(a.plano()))
                    .append(';').append(a.valorMensal().toPlainString().replace('.', ',')).append(';')
                    .append(a.pontos()).append(';').append(a.nivel()).append(';')
                    .append(a.diasSemTreinar() == null ? "mais de 28" : a.diasSemTreinar()).append(';')
                    .append(a.diasDesdeContato() == null ? "nunca" : a.diasDesdeContato()).append(';')
                    .append(celula(String.join(" | ", a.fatores()))).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Protege contra injeção de fórmula no Excel e escapa aspas e separadores. */
    static String celula(String valor) {
        String v = valor == null ? "" : valor;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return v.contains(";") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
