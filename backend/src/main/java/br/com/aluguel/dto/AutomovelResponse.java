package br.com.aluguel.dto;

import br.com.aluguel.model.Automovel;
import java.util.UUID;

public record AutomovelResponse(UUID id, String matricula, int ano, String marca, String modelo, String placa) {
    public static AutomovelResponse de(Automovel automovel) {
        return new AutomovelResponse(automovel.getId(), automovel.getMatricula(), automovel.getAno(),
            automovel.getMarca(), automovel.getModelo(), automovel.getPlaca());
    }
}
