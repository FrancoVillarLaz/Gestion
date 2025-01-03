package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    private Long numero;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;


    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    private Long documento;

    private Integer lineas;

    @OneToMany(mappedBy = "cliente")
    private List<Llamadas> llamadas;

    @OneToMany(mappedBy = "cliente")
    private List<Tramite> tramites;

}
