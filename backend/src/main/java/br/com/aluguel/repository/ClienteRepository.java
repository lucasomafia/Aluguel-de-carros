package br.com.aluguel.repository;

import br.com.aluguel.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByCpf(String cpf);
}
