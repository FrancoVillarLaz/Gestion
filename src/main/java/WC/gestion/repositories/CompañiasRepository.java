package WC.gestion.repositories;

import WC.gestion.persistencia.entities.Compañias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompañiasRepository extends JpaRepository<Compañias, Long> {
    Optional<Compañias> findById(Integer id);
}
