package br.com.aluguel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ClienteRequest(
    @NotBlank @Size(max = 30) String rg,
    @NotBlank @Pattern(regexp = "[0-9]{11}", message = "deve conter 11 dígitos") String cpf,
    @NotBlank @Size(max = 255) String endereco,
    @NotBlank @Size(max = 120) String profissao,
    @NotNull @Size(max = 3) List<@Valid VinculoRequest> vinculos
) {}
