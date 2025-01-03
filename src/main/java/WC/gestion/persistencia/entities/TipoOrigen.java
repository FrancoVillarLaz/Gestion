package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tipo_origen")
public class TipoOrigen {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_origen_seq")
    @SequenceGenerator(name = "tipo_origen_seq", sequenceName = "tipo_origen_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "tipo_origen")
    private String tipoOrigen;

    @OneToMany(mappedBy = "origen_corte")
    @JsonBackReference
    private List<Llamadas> llamada;
}
