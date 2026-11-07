package br.com.aluguel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contratos_credito")
public class ContratoCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, unique = true)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banco_id", nullable = false)
    private Usuario banco;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private int parcelas;

    @Column(name = "concedido_em", nullable = false)
    private OffsetDateTime concedidoEm;

    protected ContratoCredito() {}

    public ContratoCredito(Contrato contrato, Usuario banco, BigDecimal valor, int parcelas) {
        this.contrato = contrato;
        this.banco = banco;
        this.valor = valor;
        this.parcelas = parcelas;
        this.concedidoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public Usuario getBanco() { return banco; }
    public BigDecimal getValor() { return valor; }
    public int getParcelas() { return parcelas; }
    public OffsetDateTime getConcedidoEm() { return concedidoEm; }
}
