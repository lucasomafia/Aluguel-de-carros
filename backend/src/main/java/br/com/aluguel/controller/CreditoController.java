package br.com.aluguel.controller;

import br.com.aluguel.dto.CreditoRequest;
import br.com.aluguel.dto.CreditoResponse;
import br.com.aluguel.service.CreditoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/creditos")
public class CreditoController {
    private final CreditoService servico;

    public CreditoController(CreditoService servico) { this.servico = servico; }

    @GetMapping("/me")
    public List<CreditoResponse> listarMeus(Authentication autenticacao) {
        return servico.listarMeus(autenticacao.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('BANCO')")
    public List<CreditoResponse> listarParaBancos(Authentication autenticacao) {
        return servico.listarParaBancos(autenticacao.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('BANCO')")
    public ResponseEntity<CreditoResponse> conceder(Authentication autenticacao, @Valid @RequestBody CreditoRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.conceder(autenticacao.getName(), dados));
    }
}
