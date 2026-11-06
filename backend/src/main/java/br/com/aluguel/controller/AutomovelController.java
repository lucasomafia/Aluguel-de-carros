package br.com.aluguel.controller;

import br.com.aluguel.dto.AutomovelRequest;
import br.com.aluguel.dto.AutomovelResponse;
import br.com.aluguel.service.AutomovelService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/automoveis")
public class AutomovelController {
    private final AutomovelService servico;

    public AutomovelController(AutomovelService servico) { this.servico = servico; }

    @GetMapping
    public List<AutomovelResponse> listar() { return servico.listar(); }

    @GetMapping("/{id}")
    public AutomovelResponse consultar(@PathVariable UUID id) { return servico.consultar(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public ResponseEntity<AutomovelResponse> cadastrar(Authentication autenticacao, @Valid @RequestBody AutomovelRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.cadastrar(autenticacao.getName(), dados));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public AutomovelResponse modificar(@PathVariable UUID id, @Valid @RequestBody AutomovelRequest dados) {
        return servico.modificar(id, dados);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'BANCO')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
