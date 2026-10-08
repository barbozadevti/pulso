package dev.barboza.pulso.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Passagem pela catraca. Sem saída registrada = a pessoa ainda está dentro da unidade. */
@Entity
@Table(name = "acesso")
public class Acesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(nullable = false)
    private LocalDateTime entrada;

    private LocalDateTime saida;

    protected Acesso() {
    }

    public Acesso(Aluno aluno, Unidade unidade, LocalDateTime entrada) {
        this.aluno = aluno;
        this.unidade = unidade;
        this.entrada = entrada;
    }

    public void sair(LocalDateTime quando) {
        this.saida = quando;
    }

    public boolean dentro() {
        return saida == null;
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public Unidade getUnidade() { return unidade; }
    public LocalDateTime getEntrada() { return entrada; }
    public LocalDateTime getSaida() { return saida; }
}
