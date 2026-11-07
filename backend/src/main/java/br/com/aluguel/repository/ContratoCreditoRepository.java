package br.com.aluguel.repository;

import br.com.aluguel.model.ContratoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ContratoCreditoRepository extends JpaRepository<ContratoCredito, UUID> {
    List<ContratoCredito> findByContratoPedidoClienteIdOrderByConcedidoEmDesc(UUID clienteId);
    List<ContratoCredito> findAllByOrderByConcedidoEmDesc();
    boolean existsByContratoId(UUID contratoId);
}
