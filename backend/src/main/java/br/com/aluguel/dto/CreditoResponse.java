package br.com.aluguel.dto;

import br.com.aluguel.model.ContratoCredito;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreditoResponse(
    UUID id,
    UUID contratoId,
    String banco,
    BigDecimal valor,
    int parcelas,
    OffsetDateTime concedidoEm
) {
    public static CreditoResponse de(ContratoCredito credito) {
        return new CreditoResponse(credito.getId(), credito.getContrato().getId(), credito.getBanco().getNome(),
            credito.getValor(), credito.getParcelas(), credito.getConcedidoEm());
    }
}
