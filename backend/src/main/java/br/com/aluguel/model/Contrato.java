package br.com.aluguel.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contratos")
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Usuario proprietario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_propriedade", nullable = false, length = 20)
    private TipoPropriedade tipoPropriedade;

    @Column(name = "executado_em", nullable = false)
    private OffsetDateTime executadoEm;

    protected Contrato() {}

    public Contrato(Pedido pedido, Usuario proprietario, TipoPropriedade tipoPropriedade) {
        this.pedido = pedido;
        this.proprietario = proprietario;
        this.tipoPropriedade = tipoPropriedade;
        this.executadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Usuario getProprietario() { return proprietario; }
    public TipoPropriedade getTipoPropriedade() { return tipoPropriedade; }
    public OffsetDateTime getExecutadoEm() { return executadoEm; }
}
