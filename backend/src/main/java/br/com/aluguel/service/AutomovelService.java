package br.com.aluguel.service;

import br.com.aluguel.dto.AutomovelRequest;
import br.com.aluguel.dto.AutomovelResponse;
import br.com.aluguel.model.Automovel;
import br.com.aluguel.model.Usuario;
import br.com.aluguel.repository.AutomovelRepository;
import br.com.aluguel.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AutomovelService {
    private final AutomovelRepository automoveis;
    private final UsuarioRepository usuarios;

    public AutomovelService(AutomovelRepository automoveis, UsuarioRepository usuarios) {
        this.automoveis = automoveis;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<AutomovelResponse> listar() {
        return automoveis.findAllByOrderByMarcaAscModeloAsc().stream().map(AutomovelResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public AutomovelResponse consultar(UUID id) { return AutomovelResponse.de(buscar(id)); }

    @Transactional
    public AutomovelResponse cadastrar(String nomeUsuario, AutomovelRequest dados) {
        Usuario proprietario = usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Automovel automovel = new Automovel(proprietario);
        atualizar(automovel, dados);
        return AutomovelResponse.de(automoveis.saveAndFlush(automovel));
    }

    @Transactional
    public AutomovelResponse modificar(UUID id, AutomovelRequest dados) {
        Automovel automovel = buscar(id);
        atualizar(automovel, dados);
        return AutomovelResponse.de(automoveis.saveAndFlush(automovel));
    }

    @Transactional
    public void excluir(UUID id) {
        automoveis.delete(buscar(id));
        automoveis.flush();
    }

    private void atualizar(Automovel automovel, AutomovelRequest dados) {
        if (dados.ano() > Year.now().getValue() + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano do automóvel inválido.");
        }
        String matricula = dados.matricula().strip().toUpperCase(Locale.ROOT);
        String placa = dados.placa().strip().toUpperCase(Locale.ROOT);
        UUID id = automovel.getId();
        if (automoveis.findByMatricula(matricula).filter(existente -> !existente.getId().equals(id)).isPresent()
            || automoveis.findByPlaca(placa).filter(existente -> !existente.getId().equals(id)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula ou placa já cadastrada.");
        }
        automovel.atualizar(matricula, dados.ano(), dados.marca().strip(), dados.modelo().strip(), placa);
    }

    private Automovel buscar(UUID id) {
        return automoveis.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automóvel não encontrado."));
    }
}
