package WC.gestion.persistencia.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tipo_tramites")
public class TipoTramites {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_tramites_seq")
    @SequenceGenerator(name = "tipo_tramites_seq", sequenceName = "tipo_tramites_id_seq", allocationSize = 1)
    private Integer id;

    private String tipo;

    @OneToMany(mappedBy = "tipoTramite")
    private List<Tramite> tramites;
}
