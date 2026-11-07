package br.com.aluguel.repository;

import br.com.aluguel.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContratoRepository extends JpaRepository<Contrato, UUID> {
    List<Contrato> findByPedidoClienteIdOrderByExecutadoEmDesc(UUID clienteId);
    List<Contrato> findAllByOrderByExecutadoEmDesc();
    boolean existsByPedidoId(UUID pedidoId);

    @Query("select (count(c) > 0) from Contrato c where c.pedido.automovel.id = :automovelId " +
        "and c.pedido.dataRetirada < :devolucao and c.pedido.dataDevolucao > :retirada")
    boolean existeConflitoDeDatas(@Param("automovelId") UUID automovelId,
                                 @Param("retirada") LocalDate retirada,
                                 @Param("devolucao") LocalDate devolucao);
}
