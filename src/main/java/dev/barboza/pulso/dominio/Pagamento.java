package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Herança com uma tabela só (SINGLE_TABLE): a coluna "forma" diz qual subclasse é a linha.
 * Rápido de consultar, ao custo de colunas anuláveis (txid, final_cartao, linha_digitavel).
 */
@Entity
@Table(name = "pagamento")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "forma", length = 10)
public abstract class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "pago_em", nullable = false)
    private LocalDateTime pagoEm;

    protected Pagamento() {
    }

    protected Pagamento(BigDecimal valor, LocalDateTime pagoEm) {
        this.valor = valor;
        this.pagoEm = pagoEm;
    }

    public abstract String forma();

    /** Texto que identifica o meio de pagamento (chave, final do cartão, linha digitável). */
    public abstract String referencia();

    public Long getId() { return id; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getPagoEm() { return pagoEm; }
}
