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
import java.util.*;

import WC.gestion.persistencia.entities.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static WC.gestion.util.MapUtil.*;
@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTramitesRepository tipoTramitesRepository;
    private final CompañiasRepository compañiasRepository;

    private static final String LOG_PATH = "clientes_no_encontrados.log";

    @Transactional
    public void agregarTramites(List<TramiteDTO> tramitesDTO) {
        List<Tramite> tramites = tramitesDTO.stream().map(this::convertirDTOaEntidad).toList();
        tramiteRepository.saveAll(tramites); // Guarda todo el lote en una sola operación
    }

    @Transactional
    public void agregarTramitesDesdeCsv(InputStream inputStream) throws IOException {
        List<TramiteDTO> tramitesDto = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        int batchSize = 500_000; // Tamaño de lote

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

            for (CSVRecord registro : parser) {
                try {
                    // Procesar cada registro
                    TramiteDTO tramiteDto = procesarRegistro(registro);
                    if (tramiteDto != null) {
                        tramitesDto.add(tramiteDto);
                    }

                    // Procesar lote si se alcanza el tamaño
                    if (tramitesDto.size() == batchSize) {
                        guardarLote(tramitesDto, errores);
                        tramitesDto.clear();
                    }
                } catch (Exception e) {
                    errores.add("Error procesando la fila: " + registro.getRecordNumber() + " - " + e.getMessage());
                }
            }

            // Procesar el último lote si queda algún registro
            if (!tramitesDto.isEmpty()) {
                guardarLote(tramitesDto, errores);
            }

            // Guardar errores en archivo
            if (!errores.isEmpty()) {
                guardarErroresEnArchivo(errores);
            }
        }
    }

    private void guardarLote(List<TramiteDTO> lote, List<String> errores) {
        try {
            List<Tramite> tramites = lote.stream()
                    .map(this::convertirDTOaEntidad)
                    .filter(Objects::nonNull) // Ignorar entidades nulas
                    .toList();
            tramiteRepository.saveAll(tramites);
        } catch (Exception e) {
            errores.add("Error procesando lote: " + e.getMessage());
        }
    }

    private void guardarErroresEnArchivo(List<String> errores) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("errores.log", true))) {
            for (String error : errores) {
                writer.write(error);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error guardando errores en archivo: " + e.getMessage());
        }
    }

    private TramiteDTO procesarRegistro(CSVRecord registro) {
        try {
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
                DateTimeFormatter formatoCompleto = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                fechaFormateada = LocalDateTime.parse(fecha, formatoCompleto);
            } catch (Exception e) {
                DateTimeFormatter formatoSimplificado = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                fechaFormateada = LocalDate.parse(fecha, formatoSimplificado).atStartOfDay();
            }

            String compania = registro.get("compania");
            Integer companiaId = identificarTipoCompania(compania);

            String contrato = registro.get("contrato");
            Integer tipoContrato = identificarTipoContrato(contrato);

            String tramite = registro.isMapped("tramite") ? registro.get("tramite") : "PortOut";
            Integer tipoTramite = identificarTipoTramite(tramite);

            TramiteDTO tramiteDto = new TramiteDTO();
            tramiteDto.setClienteId(Long.parseLong(numero));
            tramiteDto.setCompaniaId(companiaId);
            tramiteDto.setFecha(fechaFormateada);
            tramiteDto.setContrato(tipoContrato);
            tramiteDto.setTipoTramiteId(tipoTramite);
            tramiteDto.setFechaConsulta(LocalDateTime.now());

            return tramiteDto;
        } catch (Exception e) {
            System.err.println("Error procesando registro: " + e.getMessage());
            return null;
        }
    }

    private Tramite convertirDTOaEntidad(TramiteDTO dto) {
        try {
            Tramite tramite = new Tramite();

            // Recuperar el cliente desde la base de datos usando el clienteId
            Optional<Cliente> clienteOptional = clienteRepository.findById(dto.getClienteId());
            if (clienteOptional.isPresent()) {
                tramite.setCliente(clienteOptional.get());
            } else {
                registrarClienteNoEncontrado(dto.getClienteId());
                return null; // Retornar null si el cliente no existe
            }

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
        } catch (Exception e) {
            System.err.println("Error convirtiendo DTO a entidad: " + e.getMessage());
            return null;
        }
    }

    private void registrarClienteNoEncontrado(Long clienteId) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_PATH, true))) {
            writer.write("Cliente no encontrado: " + clienteId);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error registrando cliente no encontrado: " + e.getMessage());
        }
    }
}
