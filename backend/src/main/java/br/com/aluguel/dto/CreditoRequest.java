package br.com.aluguel.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreditoRequest(
    @NotNull UUID contratoId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal valor,
    @NotNull @Min(1) @Max(120) Integer parcelas
) {}
