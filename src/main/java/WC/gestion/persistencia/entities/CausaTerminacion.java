package WC.gestion.persistencia.entities;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "tipo_terminacion")
public class CausaTerminacion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_terminacion_seq")
    @SequenceGenerator(name = "tipo_terminacion_seq", sequenceName = "tipo_terminacion_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "tipo_terminacion")
    private String tipoTerminacion;

    @OneToMany(mappedBy = "causa_terminacion")
    @JsonBackReference
    private List<Llamadas> llamada;
}
