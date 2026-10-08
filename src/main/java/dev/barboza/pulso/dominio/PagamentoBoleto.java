package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BOLETO")
public class PagamentoBoleto extends Pagamento {

    @Column(name = "linha_digitavel", length = 60)
    private String linhaDigitavel;

    protected PagamentoBoleto() {
    }

    public PagamentoBoleto(BigDecimal valor, LocalDateTime pagoEm, String linhaDigitavel) {
        super(valor, pagoEm);
        this.linhaDigitavel = linhaDigitavel;
    }

    @Override public String forma() { return "BOLETO"; }
    @Override public String referencia() { return linhaDigitavel; }
}
