package WC.gestion.repositories;

import WC.gestion.persistencia.entities.CausaTerminacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CausaTerminacionRepository extends JpaRepository<CausaTerminacion, Integer> {
    Optional<CausaTerminacion> findById(int id);
}
