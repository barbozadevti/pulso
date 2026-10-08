package dev.barboza.pulso.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "matricula")
public class Matricula extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @Column(nullable = false)
    private LocalDate inicio;

    private LocalDate fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusMatricula status;

    @Column(name = "motivo_cancelamento", length = 80)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("competencia")
    private List<Mensalidade> mensalidades = new ArrayList<>();

    protected Matricula() {
    }

    public Matricula(Aluno aluno, Plano plano, LocalDate inicio) {
        this.aluno = aluno;
        this.plano = plano;
        this.inicio = inicio;
        this.status = StatusMatricula.ATIVA;
    }

    public boolean vigente() {
        return status != StatusMatricula.CANCELADA;
    }

    public boolean ativa() {
        return status == StatusMatricula.ATIVA;
    }

    public void cancelar(LocalDate data, String motivo) {
        if (status == StatusMatricula.CANCELADA) {
            throw new IllegalStateException("A matrícula já está cancelada.");
        }
        this.status = StatusMatricula.CANCELADA;
        this.fim = data;
        this.motivoCancelamento = motivo;
    }

    public void trancar() {
        if (status != StatusMatricula.ATIVA) {
            throw new IllegalStateException("Só uma matrícula ativa pode ser trancada.");
        }
        this.status = StatusMatricula.TRANCADA;
    }

    public void reativar() {
        if (status != StatusMatricula.TRANCADA) {
            throw new IllegalStateException("Só uma matrícula trancada pode ser reativada.");
        }
        this.status = StatusMatricula.ATIVA;
    }

    public void mudarPlano(Plano novo) {
        this.plano = novo;
    }

    /** Gera a mensalidade da competência (1º dia do mês) com vencimento no dia indicado, se ainda não existir. */
    public Mensalidade gerarMensalidade(LocalDate competencia, LocalDate vencimento) {
        LocalDate mes = competencia.withDayOfMonth(1);
        return mensalidades.stream().filter(m -> m.getCompetencia().equals(mes)).findFirst().orElseGet(() -> {
            Mensalidade m = new Mensalidade(this, mes, plano.getValorMensal(), vencimento);
            mensalidades.add(m);
            return m;
        });
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public Plano getPlano() { return plano; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFim() { return fim; }
    public StatusMatricula getStatus() { return status; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public List<Mensalidade> getMensalidades() { return mensalidades; }
}
