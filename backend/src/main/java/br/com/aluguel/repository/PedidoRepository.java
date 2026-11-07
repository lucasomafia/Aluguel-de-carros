package br.com.aluguel.repository;

import br.com.aluguel.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByClienteIdOrderByCriadoEmDesc(UUID clienteId);
    List<Pedido> findAllByOrderByCriadoEmDesc();
}
