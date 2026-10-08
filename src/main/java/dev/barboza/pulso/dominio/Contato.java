package dev.barboza.pulso.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Um contato da equipe com um aluno em risco. Guarda a nota de risco da época para medir se a abordagem funciona. */
@Entity
@Table(name = "contato")
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CanalDeContato canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoDoContato resultado;

    @Column(length = 200)
    private String observacao;

    @Column(name = "feito_em", nullable = false)
    private LocalDateTime feitoEm;

    @Column(name = "risco_na_epoca", nullable = false)
    private int riscoNaEpoca;

    protected Contato() {
    }

    public Contato(Aluno aluno, CanalDeContato canal, ResultadoDoContato resultado, String observacao,
            LocalDateTime feitoEm, int riscoNaEpoca) {
        this.aluno = aluno;
        this.canal = canal;
        this.resultado = resultado;
        this.observacao = observacao;
        this.feitoEm = feitoEm;
        this.riscoNaEpoca = riscoNaEpoca;
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public CanalDeContato getCanal() { return canal; }
    public ResultadoDoContato getResultado() { return resultado; }
    public String getObservacao() { return observacao; }
    public LocalDateTime getFeitoEm() { return feitoEm; }
    public int getRiscoNaEpoca() { return riscoNaEpoca; }
}
