package br.com.aluguel.dto;

import br.com.aluguel.model.Pedido;
import br.com.aluguel.model.StatusPedido;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PedidoResponse(
    UUID id,
    AutomovelResponse automovel,
    LocalDate dataRetirada,
    LocalDate dataDevolucao,
    StatusPedido status,
    OffsetDateTime criadoEm,
    String parecer,
    String avaliadoPor,
    OffsetDateTime avaliadoEm
) {
    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), AutomovelResponse.de(pedido.getAutomovel()),
            pedido.getDataRetirada(), pedido.getDataDevolucao(), pedido.getStatus(), pedido.getCriadoEm(),
            pedido.getParecer(), pedido.getAvaliadoPor() == null ? null : pedido.getAvaliadoPor().getNome(),
            pedido.getAvaliadoEm());
    }
}
