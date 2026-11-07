package br.com.aluguel.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "automovel_id", nullable = false)
    private Automovel automovel;

    @Column(name = "data_retirada", nullable = false)
    private LocalDate dataRetirada;

    @Column(name = "data_devolucao", nullable = false)
    private LocalDate dataDevolucao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(length = 1000)
    private String parecer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_por_id")
    private Usuario avaliadoPor;

    @Column(name = "avaliado_em")
    private OffsetDateTime avaliadoEm;

    @Version
    @Column(nullable = false)
    private long versao;

    protected Pedido() {}

    public Pedido(Usuario cliente, Automovel automovel, LocalDate dataRetirada, LocalDate dataDevolucao) {
        this.cliente = cliente;
        this.automovel = automovel;
        this.dataRetirada = dataRetirada;
        this.dataDevolucao = dataDevolucao;
        this.status = StatusPedido.PENDENTE_ANALISE;
        this.criadoEm = OffsetDateTime.now();
    }

    public void modificar(Automovel automovel, LocalDate dataRetirada, LocalDate dataDevolucao) {
        this.automovel = automovel;
        this.dataRetirada = dataRetirada;
        this.dataDevolucao = dataDevolucao;
    }

    public void cancelar() { this.status = StatusPedido.CANCELADO; }

    public void contratar() { this.status = StatusPedido.CONTRATADO; }

    public void avaliar(Usuario agente, boolean aprovado, String parecer) {
        this.status = aprovado ? StatusPedido.APROVADO_AGUARDANDO_CONTRATO : StatusPedido.REJEITADO;
        this.parecer = parecer;
        this.avaliadoPor = agente;
        this.avaliadoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Usuario getCliente() { return cliente; }
    public Automovel getAutomovel() { return automovel; }
    public LocalDate getDataRetirada() { return dataRetirada; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public StatusPedido getStatus() { return status; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public String getParecer() { return parecer; }
    public Usuario getAvaliadoPor() { return avaliadoPor; }
    public OffsetDateTime getAvaliadoEm() { return avaliadoEm; }
}
