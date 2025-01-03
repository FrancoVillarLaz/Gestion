package WC.gestion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TramiteDTO {
        private Long clienteId;
        private Integer contrato;
        private Integer tipoTramiteId;
        private LocalDateTime fecha;
        private LocalDateTime fechaConsulta;
        private Integer companiaId;
}