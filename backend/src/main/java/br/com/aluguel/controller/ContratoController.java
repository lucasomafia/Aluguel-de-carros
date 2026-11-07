package br.com.aluguel.controller;

import br.com.aluguel.dto.ContratoRequest;
import br.com.aluguel.dto.ContratoResponse;
import br.com.aluguel.service.ContratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {
    private final ContratoService servico;

    public ContratoController(ContratoService servico) { this.servico = servico; }

    @GetMapping("/me")
    public List<ContratoResponse> listarMeus(Authentication autenticacao) {
        return servico.listarMeus(autenticacao.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public List<ContratoResponse> listarParaAgentes(Authentication autenticacao) {
        return servico.listarParaAgentes(autenticacao.getName());
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> executar(Authentication autenticacao, @Valid @RequestBody ContratoRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.executar(autenticacao.getName(), dados));
    }
}
