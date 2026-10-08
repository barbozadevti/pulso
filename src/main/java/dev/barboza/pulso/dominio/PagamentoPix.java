package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PIX")
public class PagamentoPix extends Pagamento {

    @Column(name = "txid", length = 35)
    private String txid;

    protected PagamentoPix() {
    }

    public PagamentoPix(BigDecimal valor, LocalDateTime pagoEm, String txid) {
        super(valor, pagoEm);
        this.txid = txid;
    }

    @Override public String forma() { return "PIX"; }
    @Override public String referencia() { return txid; }
}
