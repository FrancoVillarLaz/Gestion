package WC.gestion.service;


import WC.gestion.dto.LlamadasDTO;
import WC.gestion.persistencia.entities.CausaTerminacion;
import WC.gestion.persistencia.entities.Cliente;
import WC.gestion.persistencia.entities.Llamadas;
import WC.gestion.persistencia.entities.TipoOrigen;
import WC.gestion.persistencia.entities.TipoTipificacion;
import WC.gestion.repositories.*;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static WC.gestion.util.MapUtil.*;
import static WC.gestion.util.NumberUtil.*;

@Service
@RequiredArgsConstructor
public class LlamadasService {
    private final LlamadasRepository llamadasRepository;
    private final ClienteRepository clienteRepository;
    private final TipoTipificacionRepository tipoTipificacionRepository;
    private final TipoOrigenRepository tipoOrigenRepository;
    private final CausaTerminacionRepository causaTerminacionRepository;

    private final TransactionTemplate transactionTemplate; // Para manejo manual de transacciones

    public void agregarLlamadaDesdeCsv(InputStream inputStream) throws IOException {
        List<String> errores = new ArrayList<>();
        List<LlamadasDTO> batch = new ArrayList<>();
        int batchSize = 50_000;
        int threadPoolSize = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withDelimiter(';')
                     .withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    LlamadasDTO llamadasDTO = crearLlamadaDTO(record);
                    if (llamadasDTO != null) {
                        batch.add(llamadasDTO);
                    }

                    if (batch.size() == batchSize) {
                        List<LlamadasDTO> loteProcesar = new ArrayList<>(batch);
                        executor.submit(() -> guardarLoteConTransaccion(loteProcesar, errores));
                        batch.clear();
                    }
                } catch (Exception e) {
                    errores.add("Error en registro: " + record + " - Causa: " + e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                guardarLoteConTransaccion(batch, errores);
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

            if (!errores.isEmpty()) {
                registrarErrores(errores);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Error esperando la finalización de las tareas", e);
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    private void guardarLoteConTransaccion(List<LlamadasDTO> batch, List<String> errores) {
        transactionTemplate.execute(status -> {
            try {
                List<Llamadas> llamadas = batch.stream()
                        .map(this::convertDtoaEntidad)
                        .toList();

                llamadasRepository.saveAll(llamadas); // Guardar el lote
            } catch (Exception e) {
                errores.add("Error procesando lote: " + e.getMessage());
                System.err.println("Error procesando lote: " + e.getMessage());
                status.setRollbackOnly(); // Marcar la transacción para rollback
            }
            return null;
        });
    }

    private void registrarErrores(List<String> errores) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("errores_llamadas.log", true))) {
            for (String error : errores) {
                writer.write(error);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al registrar errores: " + e.getMessage());
        }
    }

    private LlamadasDTO crearLlamadaDTO(CSVRecord record) {
        try {
            String numeroCon15 = record.get("Cliente");
            if (numeroCon15 == null || numeroCon15.isEmpty()) {
                throw new IllegalArgumentException("Número vacío o nulo en el registro: " + record.getRecordNumber());
            }
            long numero = parseLongOrDefault(saca15(numeroCon15), null);

            String fechaStr = record.get("Inicio");
            if (fechaStr == null || fechaStr.isEmpty()) {
                throw new IllegalArgumentException("Fecha vacía o nula en el registro: " + record.getRecordNumber());
            }
            LocalDateTime fecha = LocalDateTime.parse(fechaStr, DateTimeFormatter.ofPattern("d/M/yyyy HH:mm:ss"));

            int duracion = parseIntOrDefault(record.get("Duración"), 0);

            String causaTerminacionStr = record.get("Causa Terminación");
            int causaTerminacion = identificarTipoTerminacion(causaTerminacionStr);

            String tipoOrigenStr = record.get("Origen Corte");
            System.out.println(tipoOrigenStr);
            int tipoOrigen = identificarTipoOrigen(tipoOrigenStr);

            String tipoTipificacionStr = record.get("Tipificación");
            int tipoTipificacion = identificarTipoTipificacion(tipoTipificacionStr);

            LlamadasDTO llamadasDTO = new LlamadasDTO();
            llamadasDTO.setNumero(numero);
            llamadasDTO.setFecha(fecha);
            llamadasDTO.setDuracion(duracion);
            llamadasDTO.setCausaTerminacion(causaTerminacion);
            llamadasDTO.setTipoOrigen(tipoOrigen);
            llamadasDTO.setTipoTipificacion(tipoTipificacion);

            return llamadasDTO;
        } catch (Exception e) {
            System.err.println("Error procesando llamada: " + e.getMessage());
            return null;
        }
    }

    private Llamadas convertDtoaEntidad(LlamadasDTO llamadasDTO) {
        Llamadas llamadas = new Llamadas();

        Cliente cliente = clienteRepository.findById(llamadasDTO.getNumero())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con número: " + llamadasDTO.getNumero()));
        llamadas.setCliente(cliente);

        TipoTipificacion tipificacion = tipoTipificacionRepository.findById(llamadasDTO.getTipoTipificacion())
                .orElseThrow(() -> new IllegalArgumentException("Tipificación no encontrada con ID: " + llamadasDTO.getTipoTipificacion()));
        llamadas.setTipoTipificacion(tipificacion);

        TipoOrigen tipoOrigen = tipoOrigenRepository.findById(llamadasDTO.getTipoOrigen())
                .orElseThrow(() -> new IllegalArgumentException("Origen no encontrado con ID: " + llamadasDTO.getTipoOrigen()));
        llamadas.setTipoOrigen(tipoOrigen);

        CausaTerminacion causaTerminacion = causaTerminacionRepository.findById(llamadasDTO.getCausaTerminacion())
                .orElseThrow(() -> new IllegalArgumentException("Causa de terminación no encontrada con ID: " + llamadasDTO.getCausaTerminacion()));
        llamadas.setCausaTerminacion(causaTerminacion);

        llamadas.setFecha(Timestamp.valueOf(llamadasDTO.getFecha()));
        llamadas.setDuracion(llamadasDTO.getDuracion());

        return llamadas;
    }
}