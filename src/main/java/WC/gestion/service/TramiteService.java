package WC.gestion.service;


import WC.gestion.dto.TramiteDTO;
import WC.gestion.persistencia.entities.Tramite;
import WC.gestion.repositories.ClienteRepository;
import WC.gestion.repositories.CompañiasRepository;
import WC.gestion.repositories.TipoTramitesRepository;
import WC.gestion.repositories.TramiteRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import WC.gestion.persistencia.entities.*;
import java.util.Optional;

import static WC.gestion.util.MapUtil.*;

@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTramitesRepository tipoTramitesRepository;
    private final CompañiasRepository compañiasRepository;

    @Transactional
    public void agregarTramites(List<TramiteDTO> tramitesDTO) {
        List<Tramite> tramites = tramitesDTO.stream().map(this::convertirDTOaEntidad).toList();
        tramiteRepository.saveAll(tramites);  // Utiliza saveAll para guardar en batch
    }

    @Transactional
    public void agregarTramitesDesdeCsv(InputStream inputStream) throws IOException {
        List<TramiteDTO> tramitesDto = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withDelimiter(';').withFirstRecordAsHeader())) {

            for (CSVRecord registro : parser) {
                try {
                    // Validar y obtener los datos necesarios
                    String numero = registro.get("numero");
                    if (numero == null || numero.isEmpty()) {
                        throw new IllegalArgumentException("Número vacío o nulo en la fila: " + registro.getRecordNumber());
                    }

                    String fecha = registro.get("fecha_portout");
                    if (fecha == null || fecha.isEmpty()) {
                        throw new IllegalArgumentException("Fecha vacía o nula en la fila: " + registro.getRecordNumber());
                    }

                    LocalDateTime fechaFormateada;
                    try {
                        // Intentar con formato completo (con hora)
                        DateTimeFormatter formatoCompleto = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        fechaFormateada = LocalDateTime.parse(fecha, formatoCompleto);
                    } catch (Exception e) {
                        try {
                            // Intentar con formato simplificado (sin hora)
                            DateTimeFormatter formatoSimplificado = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            fechaFormateada = LocalDate.parse(fecha, formatoSimplificado).atStartOfDay();
                        } catch (Exception e2) {
                            throw new IllegalArgumentException("Formato de fecha inválido en la fila: " + registro.getRecordNumber() + " - Valor: " + fecha);
                        }
                    }
                    LocalDateTime fechaConsulta = LocalDateTime.now();

                    String compania = registro.get("compania");
                    Integer companiaId = identificarTipoCompania(compania);

                    String contrato = registro.get("contrato");
                    Integer tipoContrato = identificarTipoContrato(contrato);

                    // Manejar la columna "tramite" con un valor por defecto
                    String tramite = registro.isMapped("tramite") ? registro.get("tramite") : "PortOut";
                    Integer tipoTramite = identificarTipoTramite(tramite);

                    // Crear el DTO
                    TramiteDTO tramiteDto = new TramiteDTO();
                    tramiteDto.setClienteId(Long.parseLong(numero));
                    tramiteDto.setCompaniaId(companiaId);
                    tramiteDto.setFecha(fechaFormateada);
                    tramiteDto.setContrato(tipoContrato);
                    tramiteDto.setTipoTramiteId(tipoTramite);
                    tramiteDto.setFechaConsulta(fechaConsulta);

                    tramitesDto.add(tramiteDto);

                } catch (Exception e) {
                    // Registrar errores en lugar de detener la ejecución
                    errores.add("Error procesando la fila: " + registro.getRecordNumber() + " - " + e.getMessage());
                }
            }
        }

        // Convertir DTOs a entidades y guardar
        List<Tramite> tramites = tramitesDto.stream()
                .map(this::convertirDTOaEntidad)
                .toList();
        tramiteRepository.saveAll(tramites);

        // Mostrar los errores si hay alguno
        if (!errores.isEmpty()) {
            System.out.println("Errores detectados durante la carga del CSV:");
            errores.forEach(System.out::println);
        }
    }



    private Tramite convertirDTOaEntidad(TramiteDTO dto) {
        Tramite tramite = new Tramite();

        // Recuperar el cliente desde la base de datos usando el clienteId
        Optional<Cliente> clienteOptional = clienteRepository.findById(dto.getClienteId());
        clienteOptional.ifPresent(tramite::setCliente);

        // Recuperar el tipo de tramite desde la base de datos usando el tipoTramiteId
        Optional<TipoTramites> tipoTramiteOptional = tipoTramitesRepository.findById(dto.getTipoTramiteId());
        tipoTramiteOptional.ifPresent(tramite::setTipoTramite);

        // Recuperar la compañía desde la base de datos usando el companiaId
        Optional<Compañias> compañiasOptional = compañiasRepository.findById(dto.getCompaniaId());
        compañiasOptional.ifPresent(tramite::setCompania);

        // Setear otros atributos del tramite
        tramite.setFecha(dto.getFecha());
        tramite.setFechaConsulta(dto.getFechaConsulta());
        tramite.setContrato(dto.getContrato());

        return tramite;
    }


}
