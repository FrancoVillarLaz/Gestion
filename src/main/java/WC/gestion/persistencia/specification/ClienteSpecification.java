package WC.gestion.persistencia.specification;

import WC.gestion.persistencia.entities.Cliente;
import WC.gestion.persistencia.entities.Llamadas;
import WC.gestion.persistencia.entities.Tramite;
import WC.gestion.util.MapUtil;
import WC.gestion.util.DocumentoUtil;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Join;

public class ClienteSpecification {
    public static Specification<Cliente> getClientesEspecificacion(
            List<Integer> companias, Integer contrato, String tipoDocumento, boolean noEsCUIT,
            Integer tipificacion, Timestamp fechaLlamadaInicio, Timestamp fechaLlamadaFin,
            Integer duracionMinima, Integer duracionMaxima, Integer causaTerminacion
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // JOIN con trámites y llamadas
            Join<Cliente, Tramite> tramiteJoin = root.join("tramites", JoinType.LEFT);
            Join<Cliente, Llamadas> llamadasJoin = root.join("llamadas", JoinType.LEFT);

            // Filtrar por compañías de trámites
            if (companias != null && !companias.isEmpty()) {
                predicates.add(tramiteJoin.get("compania_id").in(companias));
            }

            // Filtrar por contrato
            if (contrato != null) {
                predicates.add(builder.equal(tramiteJoin.get("contrato_id"), contrato));
            }

            // Filtrar por tipo de documento
            if (tipoDocumento != null) {
                if (noEsCUIT) {
                    predicates.add(builder.notEqual(root.get("tipo_documento"), "CUIT"));
                } else {
                    predicates.add(builder.equal(root.get("tipo_documento"), tipoDocumento));
                }
            }

            // Filtrar por tipificación de llamadas
            if (tipificacion != null) {
                predicates.add(builder.equal(llamadasJoin.get("tipificacion"), tipificacion));
            }

            // Filtrar por rango de fechas de llamadas
            if (fechaLlamadaInicio != null && fechaLlamadaFin != null) {
                predicates.add(builder.between(llamadasJoin.get("fecha"), fechaLlamadaInicio, fechaLlamadaFin));
            }

            // Filtrar por duración de llamadas
            if (duracionMinima != null) {
                predicates.add(builder.greaterThanOrEqualTo(llamadasJoin.get("duracion"), duracionMinima));
            }
            if (duracionMaxima != null) {
                predicates.add(builder.lessThanOrEqualTo(llamadasJoin.get("duracion"), duracionMaxima));
            }

            // Filtrar por causa de terminación
            if (causaTerminacion != null) {
                predicates.add(builder.equal(llamadasJoin.get("causa_terminacion"), causaTerminacion));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}


