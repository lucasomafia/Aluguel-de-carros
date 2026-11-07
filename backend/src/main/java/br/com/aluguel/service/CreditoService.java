package br.com.aluguel.service;

import br.com.aluguel.dto.CreditoRequest;
import br.com.aluguel.dto.CreditoResponse;
import br.com.aluguel.model.*;
import br.com.aluguel.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class CreditoService {
    private final ContratoCreditoRepository creditos;
    private final ContratoRepository contratos;
    private final UsuarioRepository usuarios;

    public CreditoService(ContratoCreditoRepository creditos, ContratoRepository contratos, UsuarioRepository usuarios) {
        this.creditos = creditos;
        this.contratos = contratos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<CreditoResponse> listarMeus(String nomeUsuario) {
        Usuario cliente = buscarUsuario(nomeUsuario);
        if (cliente.getPerfil() != Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para clientes.");
        }
        return creditos.findByContratoPedidoClienteIdOrderByConcedidoEmDesc(cliente.getId()).stream().map(CreditoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<CreditoResponse> listarParaBancos(String nomeUsuario) {
        buscarBanco(nomeUsuario);
        return creditos.findAllByOrderByConcedidoEmDesc().stream().map(CreditoResponse::de).toList();
    }

    @Transactional
    public CreditoResponse conceder(String nomeUsuario, CreditoRequest dados) {
        Usuario banco = buscarBanco(nomeUsuario);
        Contrato contrato = contratos.findById(dados.contratoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        if (creditos.existsByContratoId(contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este contrato já possui crédito concedido.");
        }
        return CreditoResponse.de(creditos.saveAndFlush(new ContratoCredito(contrato, banco, dados.valor(), dados.parcelas())));
    }

    private Usuario buscarBanco(String nomeUsuario) {
        Usuario usuario = buscarUsuario(nomeUsuario);
        if (usuario.getPerfil() != Perfil.BANCO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para bancos.");
        }
        return usuario;
    }

    private Usuario buscarUsuario(String nomeUsuario) {
        return usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
