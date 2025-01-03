package WC.gestion.repositories;


import WC.gestion.persistencia.entities.Llamadas;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlamadasRepository extends JpaRepository<Llamadas, Long> {
}
