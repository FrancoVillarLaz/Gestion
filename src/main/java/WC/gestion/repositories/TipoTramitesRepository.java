package WC.gestion.repositories;

import WC.gestion.persistencia.entities.TipoTramites;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoTramitesRepository extends JpaRepository<TipoTramites, Long> {
    Optional<TipoTramites> findById(Integer id);
}
