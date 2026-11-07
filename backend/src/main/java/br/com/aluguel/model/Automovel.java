package br.com.aluguel.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "automoveis")
public class Automovel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String matricula;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false, length = 80)
    private String marca;

    @Column(nullable = false, length = 80)
    private String modelo;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Usuario proprietario;

    protected Automovel() {}

    public Automovel(Usuario proprietario) { this.proprietario = proprietario; }

    public void atualizar(String matricula, int ano, String marca, String modelo, String placa) {
        this.matricula = matricula;
        this.ano = ano;
        this.marca = marca;
        this.modelo = modelo;
        this.placa = placa;
    }

    public void alterarProprietario(Usuario proprietario) { this.proprietario = proprietario; }

    public UUID getId() { return id; }
    public String getMatricula() { return matricula; }
    public int getAno() { return ano; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getPlaca() { return placa; }
    public Usuario getProprietario() { return proprietario; }
}
