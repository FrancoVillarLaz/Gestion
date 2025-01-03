package WC.gestion.persistencia.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "companias")
public class Compañias {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "companias_seq")
    @SequenceGenerator(name = "companias_seq", sequenceName = "companias_id_seq", allocationSize = 1)
    private Integer id;

    private String compania;

    @OneToMany(mappedBy = "compania")
    private List<Tramite> tramites;

}
