package br.com.aluguel.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PedidoRequest(
    @NotNull UUID automovelId,
    @NotNull LocalDate dataRetirada,
    @NotNull LocalDate dataDevolucao
) {}
