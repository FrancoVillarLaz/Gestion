package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tipo_contrato")
public class TipoContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_contrato_seq")
    @SequenceGenerator(name = "tipo_contrato_seq", sequenceName = "tipo_contrato_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "tipo_contrato", nullable = false)
    private String tipoContrato;

    @OneToMany(mappedBy = "contrato")
    @JsonBackReference
    private List<Tramite> tramites;

}
