package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "llamadas")
public class Llamadas {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "llamadas_seq")
    @SequenceGenerator(name = "llamadas_seq", sequenceName = "llamadas_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "numero", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "tipificacion", nullable = false)
    @JsonManagedReference
    private TipoTipificacion tipoTipificacion;

    private Timestamp fecha;

    private Integer duracion;

    @ManyToOne
    @JoinColumn(name = "origen_corte")
    @JsonManagedReference
    private TipoOrigen tipoOrigen;

    @ManyToOne
    @JoinColumn(name = "causa_terminacion")
    @JsonManagedReference
    private CausaTerminacion causaTerminacion;
}
