package WC.gestion.persistencia.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tipo_documento")
public class TipoDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_documento_seq")
    @SequenceGenerator(name = "tipo_documento_seq", sequenceName = "tipo_documento_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    @OneToMany(mappedBy = "tipoDocumento")
    @JsonBackReference
    private List<Cliente> clientes;
}
