package dev.barboza.pulso.servico;

/** Validação e formatação de CPF (dígitos verificadores do módulo 11). */
public final class Cpf {

    private Cpf() {
    }

    public static String somenteDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    public static String formatar(String cpf) {
        String d = somenteDigitos(cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Gera um CPF válido a partir de 9 dígitos-base (usado só nos dados de demonstração). */
    public static String comBase(int base) {
        String nove = String.format("%09d", Math.floorMod(base, 1_000_000_000));
        if (nove.chars().distinct().count() == 1) {
            nove = nove.substring(0, 8) + (char) ('0' + (nove.charAt(8) - '0' + 1) % 10);
        }
        int d1 = digito(nove, 9);
        int d2 = digito(nove + d1, 10);
        return formatar(nove + d1 + d2);
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
