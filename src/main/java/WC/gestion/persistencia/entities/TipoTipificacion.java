package WC.gestion.persistencia.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tipo_tipificacion")
public class TipoTipificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_tipificacion_seq")
    @SequenceGenerator(name = "tipo_tipificacion_seq", sequenceName = "tipos_tipificacion_id_seq", allocationSize = 1)
    private Integer id;

    private String tipo;

    @OneToMany(mappedBy = "tipoTipificacion")
    private List<Llamadas> llamadas;

}
