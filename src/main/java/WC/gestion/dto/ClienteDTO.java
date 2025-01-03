package WC.gestion.dto;

import WC.gestion.persistencia.entities.TipoDocumento;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteDTO {
    private Long numero;
    private String nombreCompleto;
    private Integer tipoDocumentoId;  // Cambiado a Integer
    private Long documento;
    private Integer lineas;
}
