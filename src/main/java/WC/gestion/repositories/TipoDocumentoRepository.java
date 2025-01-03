package WC.gestion.repositories;

import WC.gestion.persistencia.entities.TipoDocumento;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {
    Optional<TipoDocumento> findById(int id);
    Optional<TipoDocumento> findByTipoDocumento(String documento);
}