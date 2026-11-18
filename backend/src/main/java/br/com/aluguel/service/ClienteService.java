package br.com.aluguel.service;

import br.com.aluguel.dto.ClienteRequest;
import br.com.aluguel.dto.ClienteResponse;
import br.com.aluguel.model.Cliente;
import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.model.VinculoEmpregaticio;
import br.com.aluguel.repository.ClienteRepository;
import br.com.aluguel.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class ClienteService {
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;

    public ClienteService(ClienteRepository clientes, UsuarioRepository usuarios) {
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public ClienteResponse consultar(String nomeUsuario) {
        return ClienteResponse.de(buscarCliente(buscarUsuario(nomeUsuario).getId()));
    }

    @Transactional
    public ClienteResponse salvar(String nomeUsuario, ClienteRequest dados) {
        Usuario usuario = buscarUsuario(nomeUsuario);
        UUID id = usuario.getId();
        String cpf = dados.cpf().strip();
        if (!cpfValido(cpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido. Confira os 11 dígitos informados.");
        }
        if (clientes.findByCpf(cpf).filter(cliente -> !cliente.getUsuarioId().equals(id)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        }
        Cliente cliente = clientes.findById(id).orElseGet(() -> new Cliente(usuario));
        cliente.atualizar(dados.rg().strip(), cpf, dados.endereco().strip(), dados.profissao().strip(),
            dados.vinculos().stream().map(vinculo -> new VinculoEmpregaticio(
                vinculo.entidadeEmpregadora().strip(), vinculo.rendimento())).toList());
        return ClienteResponse.de(clientes.saveAndFlush(cliente));
    }

    @Transactional
    public void excluir(String nomeUsuario) {
        Usuario usuario = buscarUsuario(nomeUsuario);
        Cliente cliente = buscarCliente(usuario.getId());
        clientes.delete(cliente);
        clientes.flush();
    }

    static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = (soma * 10) % 11 % 10;
            if (digito != cpf.charAt(tamanho) - '0') return false;
        }
        return true;
    }

    private Usuario buscarUsuario(String nomeUsuario) {
        Usuario usuario = usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (usuario.getPerfil() != Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para clientes.");
        }
        return usuario;
    }

    private Cliente buscarCliente(UUID id) {
        return clientes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dados do cliente ainda não cadastrados."));
    }
}
