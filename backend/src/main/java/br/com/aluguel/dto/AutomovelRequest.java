package br.com.aluguel.dto;

import jakarta.validation.constraints.*;

public record AutomovelRequest(
    @NotBlank @Size(max = 40) String matricula,
    @Min(1886) int ano,
    @NotBlank @Size(max = 80) String marca,
    @NotBlank @Size(max = 80) String modelo,
    @NotBlank @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}", message = "deve conter uma placa brasileira válida no formato antigo ou Mercosul") String placa
) {}
