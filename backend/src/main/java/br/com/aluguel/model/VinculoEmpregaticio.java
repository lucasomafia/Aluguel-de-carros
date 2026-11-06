package br.com.aluguel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class VinculoEmpregaticio {
    @Column(name = "entidade_empregadora", nullable = false, length = 120)
    private String entidadeEmpregadora;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rendimento;

    protected VinculoEmpregaticio() {}

    public VinculoEmpregaticio(String entidadeEmpregadora, BigDecimal rendimento) {
        this.entidadeEmpregadora = entidadeEmpregadora;
        this.rendimento = rendimento;
    }

    public String getEntidadeEmpregadora() { return entidadeEmpregadora; }
    public BigDecimal getRendimento() { return rendimento; }
}
