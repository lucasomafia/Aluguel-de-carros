package br.com.aluguel.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String rg;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, length = 255)
    private String endereco;

    @Column(nullable = false, length = 120)
    private String profissao;

    @ElementCollection
    @CollectionTable(name = "vinculos_empregaticios", joinColumns = @JoinColumn(name = "usuario_id"))
    @OrderColumn(name = "posicao")
    private List<VinculoEmpregaticio> vinculos = new ArrayList<>();

    protected Cliente() {}

    public Cliente(Usuario usuario) { this.usuario = usuario; }

    public void atualizar(String rg, String cpf, String endereco, String profissao, List<VinculoEmpregaticio> vinculos) {
        this.rg = rg;
        this.cpf = cpf;
        this.endereco = endereco;
        this.profissao = profissao;
        this.vinculos.clear();
        this.vinculos.addAll(vinculos);
    }

    public UUID getUsuarioId() { return usuarioId; }
    public Usuario getUsuario() { return usuario; }
    public String getRg() { return rg; }
    public String getCpf() { return cpf; }
    public String getEndereco() { return endereco; }
    public String getProfissao() { return profissao; }
    public List<VinculoEmpregaticio> getVinculos() { return vinculos; }
}
