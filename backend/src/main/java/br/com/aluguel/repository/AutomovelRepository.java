package br.com.aluguel.repository;

import br.com.aluguel.model.Automovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomovelRepository extends JpaRepository<Automovel, UUID> {
    List<Automovel> findAllByOrderByMarcaAscModeloAsc();
    Optional<Automovel> findByMatricula(String matricula);
    Optional<Automovel> findByPlaca(String placa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Automovel a where a.id = :id")
    Optional<Automovel> bloquearPorId(@Param("id") UUID id);
}
