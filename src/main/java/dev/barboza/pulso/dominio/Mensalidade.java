package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "mensalidade")
public class Mensalidade extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id")
    private Matricula matricula;

    /** Sempre o dia 1º do mês a que a cobrança se refere. */
    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    /** Dono da chave estrangeira; fica nulo enquanto a mensalidade não é paga. */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "pagamento_id", unique = true)
    private Pagamento pagamento;

    protected Mensalidade() {
    }

    Mensalidade(Matricula matricula, LocalDate competencia, BigDecimal valor, LocalDate vencimento) {
        this.matricula = matricula;
        this.competencia = competencia;
        this.valor = valor;
        this.vencimento = vencimento;
    }

    public void pagar(Pagamento pagamento) {
        if (this.pagamento != null) {
            throw new IllegalStateException("A mensalidade de " + competencia + " já foi paga.");
        }
        this.pagamento = pagamento;
    }

    public boolean paga() {
        return pagamento != null;
    }

    public SituacaoMensalidade situacao(LocalDate hoje) {
        if (paga()) {
            return SituacaoMensalidade.PAGA;
        }
        return vencimento.isBefore(hoje) ? SituacaoMensalidade.ATRASADA : SituacaoMensalidade.ABERTA;
    }

    public long diasDeAtraso(LocalDate hoje) {
        return paga() || !vencimento.isBefore(hoje) ? 0 : ChronoUnit.DAYS.between(vencimento, hoje);
    }

    public Long getId() { return id; }
    public Matricula getMatricula() { return matricula; }
    public LocalDate getCompetencia() { return competencia; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getVencimento() { return vencimento; }
    public Pagamento getPagamento() { return pagamento; }
}
