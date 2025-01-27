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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        Queue<String> errores = new ConcurrentLinkedQueue<>(); // Manejo concurrente de errores
        List<LlamadasDTO> batch = new ArrayList<>();
        int batchSize = 10_000; // Tamaño del lote ajustado
        int threadPoolSize = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withDelimiter(';')
                     .withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    LlamadasDTO llamadasDTO = crearLlamadaDTO(record); // Crear la entidad DTO
                    if (llamadasDTO != null) {
                        batch.add(llamadasDTO);
                    }

                    // Procesar el lote cuando alcance el tamaño máximo
                    if (batch.size() == batchSize) {
                        procesarLote(batch, executor, errores);
                        batch.clear();
                    }
                } catch (Exception e) {
                    errores.add("Error en registro: " + record + " - Causa: " + e.getMessage());
                }
            }

            // Procesar cualquier lote restante
            if (!batch.isEmpty()) {
                procesarLote(batch, executor, errores);
            }

            // Finalizar las tareas y manejar executor
            cerrarExecutor(executor);

            // Registrar errores si existen
            if (!errores.isEmpty()) {
                registrarErrores(errores);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo CSV", e);
        }
    }

    private void procesarLote(List<LlamadasDTO> batch, ExecutorService executor, Queue<String> errores) {
        List<LlamadasDTO> loteProcesar = new ArrayList<>(batch);

        executor.submit(() -> {
            // Procesar cada registro individualmente para manejar errores sin afectar al lote completo
            List<Llamadas> llamadasValidas = new ArrayList<>();

            for (LlamadasDTO dto : loteProcesar) {
                try {
                    Llamadas llamada = convertDtoaEntidad(dto);
                    llamadasValidas.add(llamada); // Agregar llamadas válidas
                } catch (Exception e) {
                    // Registrar errores a nivel de registro individual
                    errores.add("Error procesando registro DTO: " + dto + " - Causa: " + e.getMessage());
                }
            }

            // Guardar todas las llamadas válidas en la base de datos (si hay)
            if (!llamadasValidas.isEmpty()) {
                guardarConTransaccion(llamadasValidas);
            }
        });
    }

    private void guardarConTransaccion(List<Llamadas> llamadasValidas) {
        transactionTemplate.execute(status -> {
            try {
                llamadasRepository.saveAll(llamadasValidas); // Guardar el lote en la base de datos
            } catch (Exception e) {
                System.err.println("Error al guardar lote válido: " + e.getMessage());
                status.setRollbackOnly(); // Rollback si ocurre un error en el guardado
            }
            return null;
        });
    }

    private void cerrarExecutor(ExecutorService executor) {
        executor.shutdown(); // Iniciar cierre del executor
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow(); // Forzar cierre si excedió el tiempo límite
            }
        } catch (InterruptedException e) {
            executor.shutdownNow(); // Cierre inmediato en caso de interrupción
            Thread.currentThread().interrupt();
        }
    }

    private void registrarErrores(Queue<String> errores) {
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
            // Leer y validar Cliente
            String numeroCon15 = record.get("Cliente");
            if (numeroCon15 == null || numeroCon15.isEmpty()) {
                throw new IllegalArgumentException("Número vacío o nulo en el registro: " + record.getRecordNumber());
            }
            long numero = parseLongOrDefault(saca15(numeroCon15), null);

            // Leer y validar Fecha
            String fechaStr = record.get("Inicio");
            if (fechaStr == null || fechaStr.isEmpty()) {
                throw new IllegalArgumentException("Fecha vacía o nula en el registro: " + record.getRecordNumber());
            }
            // Asegurar formato correcto de fecha (día/mes/año hora:minuto:segundo)
            LocalDateTime fecha = LocalDateTime.parse(fechaStr, DateTimeFormatter.ofPattern("d/M/yyyy HH:mm:ss"));

            // Leer y validar Duración
            int duracion = parseIntOrDefault(record.get("Duración"), 0);

            // Leer y validar otros campos importantes
            String causaTerminacionStr = record.get("Causa Terminación");
            int causaTerminacion = identificarTipoTerminacion(causaTerminacionStr);

            String tipoOrigenStr = record.get("Origen Corte");
            int tipoOrigen = identificarTipoOrigen(tipoOrigenStr);

            String tipoTipificacionStr = record.get("Tipificación");
            int tipoTipificacion = identificarTipoTipificacion(tipoTipificacionStr);

            // Crear DTO con datos relevantes
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

        // Validar Cliente
        Cliente cliente = clienteRepository.findById(llamadasDTO.getNumero())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado para el número: " + llamadasDTO.getNumero()));
        llamadas.setCliente(cliente);

        // Validar Tipificación
        TipoTipificacion tipificacion = tipoTipificacionRepository.findById(llamadasDTO.getTipoTipificacion())
                .orElseThrow(() -> new IllegalArgumentException("TipoTipificacion no encontrado para el ID: " + llamadasDTO.getTipoTipificacion()));
        llamadas.setTipoTipificacion(tipificacion);

        // Validar Origen
        TipoOrigen tipoOrigen = tipoOrigenRepository.findById(llamadasDTO.getTipoOrigen())
                .orElseThrow(() -> new IllegalArgumentException("TipoOrigen no encontrado para el ID: " + llamadasDTO.getTipoOrigen()));
        llamadas.setTipoOrigen(tipoOrigen);

        // Validar Causa de Terminación
        CausaTerminacion causaTerminacion = causaTerminacionRepository.findById(llamadasDTO.getCausaTerminacion())
                .orElseThrow(() -> new IllegalArgumentException("CausaTerminacion no encontrada para el ID: " + llamadasDTO.getCausaTerminacion()));
        llamadas.setCausaTerminacion(causaTerminacion);

        // Asignar fecha y duración
        llamadas.setFecha(Timestamp.valueOf(llamadasDTO.getFecha()));
        llamadas.setDuracion(llamadasDTO.getDuracion());

        return llamadas;
    }
}