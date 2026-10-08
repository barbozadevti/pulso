package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "avaliacao_fisica")
public class AvaliacaoFisica extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @Column(nullable = false)
    private LocalDate data;

    /** Em quilos. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    /** Em metros. */
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal altura;

    @Column(name = "percentual_gordura", precision = 4, scale = 1)
    private BigDecimal percentualGordura;

    @Column(name = "cintura_cm", precision = 5, scale = 1)
    private BigDecimal cinturaCm;

    protected AvaliacaoFisica() {
    }

    public AvaliacaoFisica(Aluno aluno, LocalDate data, BigDecimal peso, BigDecimal altura,
            BigDecimal percentualGordura, BigDecimal cinturaCm) {
        this.aluno = aluno;
        atualizar(data, peso, altura, percentualGordura, cinturaCm);
    }

    public void atualizar(LocalDate data, BigDecimal peso, BigDecimal altura, BigDecimal percentualGordura,
            BigDecimal cinturaCm) {
        this.data = data;
        this.peso = peso;
        this.altura = altura;
        this.percentualGordura = percentualGordura;
        this.cinturaCm = cinturaCm;
    }

    /** Índice de massa corporal: peso / altura². */
    public BigDecimal imc() {
        return peso.divide(altura.multiply(altura), 1, RoundingMode.HALF_UP);
    }

    /** Faixa da OMS para adultos. */
    public String faixaDoImc() {
        double v = imc().doubleValue();
        if (v < 18.5) return "Abaixo do peso";
        if (v < 25) return "Peso normal";
        if (v < 30) return "Sobrepeso";
        return "Obesidade";
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public LocalDate getData() { return data; }
    public BigDecimal getPeso() { return peso; }
    public BigDecimal getAltura() { return altura; }
    public BigDecimal getPercentualGordura() { return percentualGordura; }
    public BigDecimal getCinturaCm() { return cinturaCm; }
}
