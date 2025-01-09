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
import java.util.Queue;
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

    @Transactional
    public void agregarTramites(List<TramiteDTO> tramitesDTO) {
        List<Tramite> tramites = tramitesDTO.stream().map(this::convertirDTOaEntidad).toList();
        tramiteRepository.saveAll(tramites);  // Utiliza saveAll para guardar en batch
    }

    @Transactional
    public void agregarTramitesDesdeCsv(InputStream inputStream) throws IOException {
        List<TramiteDTO> tramitesDto = new ArrayList<>();
        Queue<String> errores = new ConcurrentLinkedQueue<>();
        int batchSize = 500_000;
        ExecutorService executorService = Executors.newFixedThreadPool(12);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

            for (CSVRecord registro : parser) {
                try {
                    TramiteDTO tramiteDto = procesarRegistro(registro);
                    tramitesDto.add(tramiteDto);

                    if (tramitesDto.size() == batchSize) {
                        enviarLoteAProcesar(tramitesDto, errores, executorService);
                        tramitesDto.clear();
                    }
                } catch (Exception e) {
                    errores.add("Error procesando la fila: " + registro.getRecordNumber() + " - " + e.getMessage());
                }
            }

            if (!tramitesDto.isEmpty()) {
                enviarLoteAProcesar(tramitesDto, errores, executorService);
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }

            if (!errores.isEmpty()) {
                guardarErroresEnArchivo(errores);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error procesando el archivo CSV", e);
        }
    }

    private void enviarLoteAProcesar(List<TramiteDTO> lote, Queue<String> errores, ExecutorService executorService) {
        executorService.submit(() -> {
            try {
                guardarLote(lote);
            } catch (Exception e) {
                errores.add("Error procesando lote: " + e.getMessage());
            }
        });
    }

    @Transactional
    private void guardarLote(List<TramiteDTO> lote) {
        List<Tramite> tramites = lote.stream()
                .map(this::convertirDTOaEntidad)
                .toList();
        tramiteRepository.saveAll(tramites);
    }

    private void guardarErroresEnArchivo(Queue<String> errores) {
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
    }
    @Transactional
    private void procesarLoteEnHilo(List<TramiteDTO> lote, List<String> errores) {
        ExecutorService executorService = Executors.newFixedThreadPool(12); // Máximo 12 hilos
        executorService.submit(() -> {
            try {
                List<Tramite> tramites = lote.stream()
                        .map(this::convertirDTOaEntidad)
                        .toList();
                tramiteRepository.saveAll(tramites); // Guardar lote en base de datos
            } catch (Exception e) {
                synchronized (errores) {
                    errores.add("Error procesando lote: " + e.getMessage());
                }
            }
        });

        executorService.shutdown();
    }


    @Transactional
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
