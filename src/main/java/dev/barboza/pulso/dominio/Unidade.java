package dev.barboza.pulso.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma filial da rede. */
@Entity
@Table(name = "unidade")
public class Unidade extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Column(nullable = false, length = 60)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    /** Pessoas ao mesmo tempo dentro da unidade. */
    @Column(nullable = false)
    private int capacidade;

    protected Unidade() {
    }

    public Unidade(String nome, String cidade, String uf, int capacidade) {
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
        this.capacidade = capacidade;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public int getCapacidade() { return capacidade; }
}
