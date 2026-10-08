package dev.barboza.pulso.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CARTAO")
public class PagamentoCartao extends Pagamento {

    @Column(name = "final_cartao", length = 4)
    private String finalCartao;

    protected PagamentoCartao() {
    }

    public PagamentoCartao(BigDecimal valor, LocalDateTime pagoEm, String finalCartao) {
        super(valor, pagoEm);
        this.finalCartao = finalCartao;
    }

    @Override public String forma() { return "CARTAO"; }
    @Override public String referencia() { return "final " + finalCartao; }
}
