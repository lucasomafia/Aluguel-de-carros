package br.com.aluguel.dto;

import br.com.aluguel.model.TipoPropriedade;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContratoRequest(
    @NotNull UUID pedidoId,
    @NotNull TipoPropriedade tipoPropriedade
) {}
