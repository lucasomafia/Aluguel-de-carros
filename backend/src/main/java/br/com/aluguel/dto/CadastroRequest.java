package br.com.aluguel.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record CadastroRequest(
    @NotBlank @Size(max = 120) String nome,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,60}",
        message = "deve ter de 3 a 60 letras, números, pontos, hífens ou sublinhados") String nomeUsuario,
    @NotBlank @Size(min = 8, max = 64) String senha
) {
    @AssertTrue(message = "a senha deve ocupar no máximo 72 bytes em UTF-8")
    public boolean isSenhaCompativelComBcrypt() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
