package br.com.aluguel.service;

import br.com.aluguel.dto.CadastroRequest;
import br.com.aluguel.dto.UsuarioResponse;
import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificadorSenha;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder codificadorSenha) {
        this.repositorio = repositorio;
        this.codificadorSenha = codificadorSenha;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest requisicao) {
        return UsuarioResponse.de(criar(requisicao.nome().strip(), requisicao.nomeUsuario(), requisicao.senha(), Perfil.CLIENTE));
    }

    @Transactional
    public Usuario criar(String nome, String nomeUsuario, String senha, Perfil perfil) {
        String nomeUsuarioNormalizado = normalizar(nomeUsuario);
        if (repositorio.existsByNomeUsuario(nomeUsuarioNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já cadastrado.");
        }
        return repositorio.saveAndFlush(new Usuario(nome, nomeUsuarioNormalizado, codificadorSenha.encode(senha), perfil));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse consultar(String nomeUsuario) {
        return UsuarioResponse.de(buscar(nomeUsuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nomeUsuario) throws UsernameNotFoundException {
        Usuario usuario = buscar(nomeUsuario);
        return User.withUsername(usuario.getNomeUsuario())
            .password(usuario.getSenhaHash())
            .roles(usuario.getPerfil().name())
            .build();
    }

    private Usuario buscar(String nomeUsuario) {
        return repositorio.findByNomeUsuario(normalizar(nomeUsuario))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }

    private String normalizar(String nomeUsuario) {
        return nomeUsuario.strip().toLowerCase(Locale.ROOT);
    }
}
