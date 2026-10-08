package dev.barboza.pulso.seguranca;

/** Exceções de acesso. O TratadorDeErros da API converte cada uma no status HTTP certo. */
public final class Excecoes {

    private Excecoes() {
    }

    /** 401: e-mail ou senha não conferem. A mensagem é a mesma para "não existe" e "senha errada". */
    public static class CredenciaisInvalidas extends RuntimeException {
        public CredenciaisInvalidas() {
            super("E-mail ou senha incorretos.");
        }
    }

    /** 429: conta bloqueada por tentativas seguidas. */
    public static class UsuarioBloqueado extends RuntimeException {
        public UsuarioBloqueado(long minutos) {
            super("Muitas tentativas. Tente de novo em " + minutos + " minuto" + (minutos == 1 ? "" : "s") + ".");
        }
    }

    /** 403: o perfil não pode fazer isso. */
    public static class AcessoNegado extends RuntimeException {
        public AcessoNegado(String mensagem) {
            super(mensagem);
        }
    }
}
