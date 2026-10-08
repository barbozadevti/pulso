package dev.barboza.pulso.servico;

/** Exceções de negócio. O TratadorDeErros da API converte cada uma no status HTTP certo. */
public final class Erros {

    private Erros() {
    }

    /** 404. */
    public static class NaoEncontrado extends RuntimeException {
        public NaoEncontrado(String mensagem) {
            super(mensagem);
        }
    }

    /** 409 ou 422: a requisição é válida, mas a regra de negócio não deixa. */
    public static class RegraDeNegocio extends RuntimeException {
        public RegraDeNegocio(String mensagem) {
            super(mensagem);
        }
    }
}
