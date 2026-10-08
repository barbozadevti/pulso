package dev.barboza.pulso.seguranca;

public enum Perfil {

    /** Visão da rede inteira: painel, risco, laboratório e todas as unidades. */
    DIRETORIA("Diretoria"),
    /** Tudo da unidade dele: painel, risco, retenção, cancelamentos, cobrança, catraca e aulas. */
    GERENTE("Gerente"),
    /** Balcão da unidade: cadastro, matrícula, recebimento, catraca e aulas. Não vê risco nem painel. */
    RECEPCAO("Recepção"),
    /** O próprio cadastro (leitura) e as próprias reservas de aula. */
    ALUNO("Aluno");

    private final String nome;

    Perfil(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    /** Quem trabalha numa unidade só (não vê as outras). */
    public boolean presoAUmaUnidade() {
        return this == GERENTE || this == RECEPCAO;
    }
}
