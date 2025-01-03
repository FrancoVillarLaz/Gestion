package WC.gestion.persistencia.specification;

import WC.gestion.persistencia.entities.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import WC.gestion.persistencia.entities.Tramite;
import jakarta.persistence.criteria.Join;

public class ClienteSpecification {
    public static Specification<Cliente> getClientesEspecificacion(
            List<String> companias, String contrato, String tipoDocumento, boolean noEsCUIT,
            String tipificacion, Date fechaPortoutInicio, Date fechaPortoutFin
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Unir la tabla de tramites
            Join<Cliente, Tramite> tramiteJoin = root.join("tramites");

            // Filtrar por compañías de los trámites
            if (companias != null && !companias.isEmpty()) {
                predicates.add(tramiteJoin.get("compania").in(companias));
            }

            // Filtrar por contrato de los trámites
            if (contrato != null) {
                predicates.add(builder.equal(tramiteJoin.get("contrato"), contrato));
            }

            // Filtrar por tipo de documento (ej. DNI, CUIT)
            if (tipoDocumento != null) {
                if (noEsCUIT) {
                    predicates.add(builder.notLike(root.get("tipoDocumento").get("tipoDocumento"), "%CUIT%"));
                } else {
                    predicates.add(builder.like(root.get("tipoDocumento").get("tipoDocumento"), "%" + tipoDocumento + "%"));
                }
            }

            // Filtrar por tipificación que no sea "venta"
            if (tipificacion != null && !tipificacion.equalsIgnoreCase("venta")) {
                predicates.add(builder.notEqual(tramiteJoin.get("tipo"), "venta"));
            }

            // Filtrar por fecha de portout (basado en la fecha del trámite)
            if (fechaPortoutInicio != null && fechaPortoutFin != null) {
                predicates.add(builder.between(tramiteJoin.get("fecha"), fechaPortoutInicio, fechaPortoutFin));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
