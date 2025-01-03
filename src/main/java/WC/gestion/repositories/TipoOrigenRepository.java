package WC.gestion.repositories;

import WC.gestion.persistencia.entities.TipoOrigen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoOrigenRepository extends JpaRepository<TipoOrigen, Integer> {
    Optional<TipoOrigen> findById(int id);
}
