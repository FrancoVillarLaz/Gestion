package WC.gestion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LlamadasDTO {
    private int id;
    private long numero;
    private int tipoTipificacion;
    private LocalDateTime fecha;
    private int duracion;
    private int tipoOrigen;
    private int causaTerminacion;
}
