package br.com.aluguel.repository;

import br.com.aluguel.model.Automovel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomovelRepository extends JpaRepository<Automovel, UUID> {
    List<Automovel> findAllByOrderByMarcaAscModeloAsc();
    Optional<Automovel> findByMatricula(String matricula);
    Optional<Automovel> findByPlaca(String placa);
}
