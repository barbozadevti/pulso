package dev.barboza.pulso.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Objeto de valor embutido na tabela do aluno (@Embeddable): não tem identidade própria. */
@Embeddable
public class Endereco {

    @Column(name = "bairro", nullable = false, length = 80)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 60)
    private String cidade;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    protected Endereco() {
    }

    public Endereco(String bairro, String cidade, String uf) {
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
    }

    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
}
