package br.com.aluguel.controller;

import br.com.aluguel.dto.ClienteRequest;
import br.com.aluguel.dto.ClienteResponse;
import br.com.aluguel.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes/me")
public class ClienteController {
    private final ClienteService servico;

    public ClienteController(ClienteService servico) { this.servico = servico; }

    @GetMapping
    public ClienteResponse consultar(Authentication autenticacao) {
        return servico.consultar(autenticacao.getName());
    }

    @PutMapping
    public ClienteResponse salvar(Authentication autenticacao, @Valid @RequestBody ClienteRequest dados) {
        return servico.salvar(autenticacao.getName(), dados);
    }

    @DeleteMapping
    public ResponseEntity<Void> excluir(Authentication autenticacao) {
        servico.excluir(autenticacao.getName());
        return ResponseEntity.noContent().build();
    }
}
