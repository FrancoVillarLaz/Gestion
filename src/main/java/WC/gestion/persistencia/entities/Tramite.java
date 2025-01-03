package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "tramites")
public class Tramite {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tramites_seq")
    @SequenceGenerator(name = "tramites_seq", sequenceName = "tramites_id_seq", allocationSize = 1)
    private Integer id;


    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime fecha;

    @Column(name = "contrato_id")
    @JsonManagedReference
    private Integer contrato;

    @Column(name = "fecha_consulta")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime fechaConsulta;

    @ManyToOne
    @JoinColumn(name = "numero", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "tipo", nullable = false)
    private TipoTramites tipoTramite;

    @ManyToOne
    @JoinColumn(name = "compania_id", nullable = false)
    private Compañias compania;

}
