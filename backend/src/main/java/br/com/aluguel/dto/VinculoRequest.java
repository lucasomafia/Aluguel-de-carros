package br.com.aluguel.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VinculoRequest(
    @NotBlank @Size(max = 120) String entidadeEmpregadora,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal rendimento
) {}
