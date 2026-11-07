package br.com.aluguel.service;

import br.com.aluguel.dto.AvaliacaoRequest;
import br.com.aluguel.dto.PedidoAgenteResponse;
import br.com.aluguel.dto.PedidoRequest;
import br.com.aluguel.dto.PedidoResponse;
import br.com.aluguel.model.*;
import br.com.aluguel.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final AutomovelRepository automoveis;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, ClienteRepository clientes,
                         AutomovelRepository automoveis) {
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.automoveis = automoveis;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMeus(String nomeUsuario) {
        Usuario usuario = buscarCliente(nomeUsuario);
        return pedidos.findByClienteIdOrderByCriadoEmDesc(usuario.getId()).stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoAgenteResponse> listarParaAgentes(String nomeUsuario) {
        buscarAgente(nomeUsuario);
        return pedidos.findAllByOrderByCriadoEmDesc().stream().map(pedido -> PedidoAgenteResponse.de(pedido,
            clientes.findById(pedido.getCliente().getId()).orElse(null))).toList();
    }

    @Transactional
    public PedidoResponse criar(String nomeUsuario, PedidoRequest dados) {
        Usuario usuario = buscarCliente(nomeUsuario);
        if (!clientes.existsById(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete seus dados de cliente antes de solicitar um automóvel.");
        }
        validarDatas(dados);
        return PedidoResponse.de(pedidos.saveAndFlush(new Pedido(usuario, buscarAutomovel(dados),
            dados.dataRetirada(), dados.dataDevolucao())));
    }

    @Transactional
    public PedidoResponse modificarMeu(String nomeUsuario, UUID id, PedidoRequest dados) {
        Usuario cliente = buscarCliente(nomeUsuario);
        Pedido pedido = buscarPedido(id);
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado.");
        }
        return modificar(pedido, dados);
    }

    @Transactional
    public PedidoResponse modificarComoAgente(String nomeUsuario, UUID id, PedidoRequest dados) {
        buscarAgente(nomeUsuario);
        return modificar(buscarPedido(id), dados);
    }

    @Transactional
    public PedidoResponse cancelarMeu(String nomeUsuario, UUID id) {
        Usuario cliente = buscarCliente(nomeUsuario);
        Pedido pedido = buscarPedido(id);
        if (!pedido.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado.");
        }
        exigirPendente(pedido);
        pedido.cancelar();
        return PedidoResponse.de(pedidos.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse avaliar(String nomeUsuario, UUID id, AvaliacaoRequest dados) {
        Usuario agente = buscarAgente(nomeUsuario);
        Pedido pedido = buscarPedido(id);
        exigirPendente(pedido);
        pedido.avaliar(agente, dados.aprovado(), dados.parecer().strip());
        return PedidoResponse.de(pedidos.saveAndFlush(pedido));
    }

    private PedidoResponse modificar(Pedido pedido, PedidoRequest dados) {
        exigirPendente(pedido);
        validarDatas(dados);
        pedido.modificar(buscarAutomovel(dados), dados.dataRetirada(), dados.dataDevolucao());
        return PedidoResponse.de(pedidos.saveAndFlush(pedido));
    }

    private void validarDatas(PedidoRequest dados) {
        if (dados.dataRetirada().isBefore(LocalDate.now()) || !dados.dataDevolucao().isAfter(dados.dataRetirada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe datas de retirada e devolução válidas.");
        }
    }

    private Automovel buscarAutomovel(PedidoRequest dados) {
        return automoveis.findById(dados.automovelId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automóvel não encontrado."));
    }

    private Pedido buscarPedido(UUID id) {
        return pedidos.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private void exigirPendente(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.PENDENTE_ANALISE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido não está mais pendente de análise.");
        }
    }

    private Usuario buscarCliente(String nomeUsuario) {
        Usuario usuario = usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (usuario.getPerfil() != Perfil.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para clientes.");
        }
        return usuario;
    }

    private Usuario buscarAgente(String nomeUsuario) {
        Usuario usuario = usuarios.findByNomeUsuario(nomeUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (usuario.getPerfil() != Perfil.EMPRESA && usuario.getPerfil() != Perfil.BANCO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido apenas para agentes.");
        }
        return usuario;
    }
}
