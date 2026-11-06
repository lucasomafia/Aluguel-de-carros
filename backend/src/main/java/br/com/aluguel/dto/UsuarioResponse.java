package br.com.aluguel.dto;

import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String nomeUsuario, Perfil perfil) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getNomeUsuario(), usuario.getPerfil());
    }
}
