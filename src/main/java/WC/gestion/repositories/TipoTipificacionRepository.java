package WC.gestion.repositories;

import WC.gestion.persistencia.entities.TipoTipificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoTipificacionRepository extends JpaRepository<TipoTipificacion,Integer> {
    Optional<TipoTipificacion> findById(Integer id);
}
