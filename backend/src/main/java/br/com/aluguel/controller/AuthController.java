package br.com.aluguel.controller;

import br.com.aluguel.dto.CadastroRequest;
import br.com.aluguel.dto.UsuarioResponse;
import br.com.aluguel.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService servico;

    public AuthController(UsuarioService servico) {
        this.servico = servico;
    }

    @PostMapping(value = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        return servico.cadastrar(requisicao);
    }

    @PostMapping("/login")
    public UsuarioResponse login(Authentication autenticacao) {
        return servico.consultar(autenticacao.getName());
    }

    @GetMapping("/me")
    public UsuarioResponse dadosUsuario(Authentication autenticacao) {
        return servico.consultar(autenticacao.getName());
    }
}
