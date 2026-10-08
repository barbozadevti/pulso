package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "plano")
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nome;

    @Column(name = "valor_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "duracao_meses", nullable = false)
    private int duracaoMeses;

    /** Plano "rede": o aluno treina em qualquer unidade. Os demais valem só na unidade de origem. */
    @Column(name = "multi_unidade", nullable = false)
    private boolean multiUnidade;

    /** Muitos-para-muitos: um plano inclui várias modalidades e uma modalidade está em vários planos. */
    @ManyToMany
    @JoinTable(name = "plano_modalidade",
            joinColumns = @JoinColumn(name = "plano_id"),
            inverseJoinColumns = @JoinColumn(name = "modalidade_id"))
    private Set<Modalidade> modalidades = new LinkedHashSet<>();

    protected Plano() {
    }

    public Plano(String nome, BigDecimal valorMensal, int duracaoMeses, boolean multiUnidade,
            Set<Modalidade> modalidades) {
        this.nome = nome;
        this.valorMensal = valorMensal;
        this.duracaoMeses = duracaoMeses;
        this.multiUnidade = multiUnidade;
        this.modalidades = new LinkedHashSet<>(modalidades);
    }

    public boolean inclui(Modalidade modalidade) {
        return modalidades.stream().anyMatch(m -> m.getId().equals(modalidade.getId()));
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getValorMensal() { return valorMensal; }
    public int getDuracaoMeses() { return duracaoMeses; }
    public boolean isMultiUnidade() { return multiUnidade; }
    public Set<Modalidade> getModalidades() { return modalidades; }
}
