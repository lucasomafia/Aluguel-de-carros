package br.com.aluguel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
    @NotNull Boolean aprovado,
    @NotBlank @Size(max = 1000) String parecer
) {}
