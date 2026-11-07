package br.com.aluguel.dto;

import br.com.aluguel.model.Cliente;
import br.com.aluguel.model.Pedido;

public record PedidoAgenteResponse(PedidoResponse pedido, ClienteResponse cliente) {
    public static PedidoAgenteResponse de(Pedido pedido, Cliente cliente) {
        return new PedidoAgenteResponse(PedidoResponse.de(pedido), cliente == null ? null : ClienteResponse.de(cliente));
    }
}
