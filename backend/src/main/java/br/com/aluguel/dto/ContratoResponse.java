package br.com.aluguel.dto;

import br.com.aluguel.model.Contrato;
import br.com.aluguel.model.Perfil;
import br.com.aluguel.model.TipoPropriedade;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContratoResponse(
    UUID id,
    UUID pedidoId,
    AutomovelResponse automovel,
    String cliente,
    LocalDate dataRetirada,
    LocalDate dataDevolucao,
    TipoPropriedade tipoPropriedade,
    String proprietario,
    Perfil perfilProprietario,
    OffsetDateTime executadoEm
) {
    public static ContratoResponse de(Contrato contrato) {
        var pedido = contrato.getPedido();
        var proprietario = contrato.getProprietario();
        return new ContratoResponse(contrato.getId(), pedido.getId(), AutomovelResponse.de(pedido.getAutomovel()),
            pedido.getCliente().getNome(), pedido.getDataRetirada(), pedido.getDataDevolucao(),
            contrato.getTipoPropriedade(), proprietario.getNome(), proprietario.getPerfil(), contrato.getExecutadoEm());
    }
}
