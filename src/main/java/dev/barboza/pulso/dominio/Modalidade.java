package dev.barboza.pulso.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "modalidade")
public class Modalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nome;

    @Column(nullable = false, length = 8)
    private String icone;

    protected Modalidade() {
    }

    public Modalidade(String nome, String icone) {
        this.nome = nome;
        this.icone = icone;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getIcone() { return icone; }
}
