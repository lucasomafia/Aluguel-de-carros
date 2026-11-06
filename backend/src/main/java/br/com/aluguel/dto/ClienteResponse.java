package br.com.aluguel.dto;

import br.com.aluguel.model.Cliente;
import br.com.aluguel.model.VinculoEmpregaticio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ClienteResponse(UUID usuarioId, String nome, String rg, String cpf, String endereco, String profissao, List<VinculoResponse> vinculos) {
    public record VinculoResponse(String entidadeEmpregadora, BigDecimal rendimento) {
        public static VinculoResponse de(VinculoEmpregaticio vinculo) {
            return new VinculoResponse(vinculo.getEntidadeEmpregadora(), vinculo.getRendimento());
        }
    }

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.getUsuarioId(), cliente.getUsuario().getNome(), cliente.getRg(), cliente.getCpf(),
            cliente.getEndereco(), cliente.getProfissao(), cliente.getVinculos().stream().map(VinculoResponse::de).toList());
    }
}
