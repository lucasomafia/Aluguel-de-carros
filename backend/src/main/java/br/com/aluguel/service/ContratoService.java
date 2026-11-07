package br.com.aluguel.service;

import br.com.aluguel.dto.ContratoRequest;
import br.com.aluguel.dto.ContratoResponse;
import br.com.aluguel.model.*;
import br.com.aluguel.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ContratoService {
    private final ContratoRepository contratos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final AutomovelRepository automoveis;

    public ContratoService(ContratoRepository contratos, PedidoRepository pedidos, UsuarioRepository usuarios,
                           AutomovelRepository automoveis) {
        this.contratos = contratos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.automoveis = automoveis;
    }

    @Transactional(readOnly = true)
    public List<ContratoResponse> listarMeus(String nomeUsuario) {
        Usuario cliente = buscarUsuario(nomeUsuario);
        if (cliente.getPerfil() != Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para clientes.");
        }
        return contratos.findByPedidoClienteIdOrderByExecutadoEmDesc(cliente.getId()).stream().map(ContratoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ContratoResponse> listarParaAgentes(String nomeUsuario) {
        Usuario agente = buscarUsuario(nomeUsuario);
        if (agente.getPerfil() == Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para agentes.");
        }
        return contratos.findAllByOrderByExecutadoEmDesc().stream().map(ContratoResponse::de).toList();
    }

    @Transactional
    public ContratoResponse executar(String nomeUsuario, ContratoRequest dados) {
        Usuario cliente = buscarUsuario(nomeUsuario);
        if (cliente.getPerfil() != Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para clientes.");
        }
        Pedido pedido = pedidos.findById(dados.pedidoId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado.");
        }
        if (pedido.getStatus() != StatusPedido.APROVADO_AGUARDANDO_CONTRATO || contratos.existsByPedidoId(pedido.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido não está disponível para contratação.");
        }
        Usuario agente = pedido.getAvaliadoPor();
        if (agente == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pedido ainda não foi avaliado.");
        }
        TipoPropriedade tipo = dados.tipoPropriedade();
        if (tipo != TipoPropriedade.CLIENTE && !tipo.name().equals(agente.getPerfil().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo de propriedade não corresponde ao agente que aprovou o pedido.");
        }
        Automovel automovel = automoveis.bloquearPorId(pedido.getAutomovel().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automóvel não encontrado."));
        if (contratos.existeConflitoDeDatas(automovel.getId(), pedido.getDataRetirada(), pedido.getDataDevolucao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este automóvel já possui contrato para o período informado.");
        }
        Usuario proprietario = tipo == TipoPropriedade.CLIENTE ? cliente : agente;
        automovel.alterarProprietario(proprietario);
        pedido.contratar();
        pedidos.saveAndFlush(pedido);
        automoveis.saveAndFlush(automovel);
        return ContratoResponse.de(contratos.saveAndFlush(new Contrato(pedido, proprietario, tipo)));
    }

    private Usuario buscarUsuario(String nomeUsuario) {
        return usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
