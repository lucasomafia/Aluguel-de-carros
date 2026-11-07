package br.com.aluguel.controller;

import br.com.aluguel.dto.AvaliacaoRequest;
import br.com.aluguel.dto.PedidoAgenteResponse;
import br.com.aluguel.dto.PedidoRequest;
import br.com.aluguel.dto.PedidoResponse;
import br.com.aluguel.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService servico;

    public PedidoController(PedidoService servico) { this.servico = servico; }

    @GetMapping("/me")
    public List<PedidoResponse> listarMeus(Authentication autenticacao) {
        return servico.listarMeus(autenticacao.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public List<PedidoAgenteResponse> listarParaAgentes(Authentication autenticacao) {
        return servico.listarParaAgentes(autenticacao.getName());
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(Authentication autenticacao, @Valid @RequestBody PedidoRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(autenticacao.getName(), dados));
    }

    @PutMapping("/me/{id}")
    public PedidoResponse modificarMeu(Authentication autenticacao, @PathVariable UUID id, @Valid @RequestBody PedidoRequest dados) {
        return servico.modificarMeu(autenticacao.getName(), id, dados);
    }

    @PatchMapping("/me/{id}/cancelar")
    public PedidoResponse cancelarMeu(Authentication autenticacao, @PathVariable UUID id) {
        return servico.cancelarMeu(autenticacao.getName(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public PedidoResponse modificarComoAgente(Authentication autenticacao, @PathVariable UUID id, @Valid @RequestBody PedidoRequest dados) {
        return servico.modificarComoAgente(autenticacao.getName(), id, dados);
    }

    @PostMapping("/{id}/avaliacao")
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public PedidoResponse avaliar(Authentication autenticacao, @PathVariable UUID id, @Valid @RequestBody AvaliacaoRequest dados) {
        return servico.avaliar(autenticacao.getName(), id, dados);
    }
}
