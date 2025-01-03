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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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



    @Transactional
    public void agregarLlamadaDesdeCsv(InputStream inputStream) throws IOException {
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withDelimiter(';') // Separador
                     .withFirstRecordAsHeader())) {

            List<LlamadasDTO> batch = new ArrayList<>();
            int batchSize = 1000; // Número de registros por lote

            for (CSVRecord record : csvParser) {
                try {
                    // Crear y validar el DTO
                    LlamadasDTO llamadasDTO = crearLlamadaDTO(record);
                    batch.add(llamadasDTO);

                    // Guardar cuando el lote alcance el tamaño definido
                    if (batch.size() == batchSize) {
                        guardarLote(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    errores.add("Error en registro: " + record + " - Causa: " + e.getMessage());
                }
            }

            // Guardar el último lote
            if (!batch.isEmpty()) {
                guardarLote(batch);
            }
        }

        if (!errores.isEmpty()) {
            System.out.println("Errores detectados:");
            errores.forEach(System.out::println);
        }
    }


    public void guardarLote(List<LlamadasDTO> batch) {
        List<Llamadas> llamadas = batch.stream()
                .map(this::convertDtoaEntidad)
                .toList();
        llamadasRepository.saveAll(llamadas);
    }


    private LlamadasDTO crearLlamadaDTO(CSVRecord record) {
        // Validación y conversión de datos del CSV al DTO
        String numeroCon15 = record.get("Cliente");
        if (numeroCon15 == null || numeroCon15.isEmpty()) {
            throw new IllegalArgumentException("Número vacío o nulo en el registro: " + record.getRecordNumber());
        }
        long numero = parseLongOrDefault(saca15(numeroCon15), null);

        String fechaStr = record.get("Inicio");
        if (fechaStr == null || fechaStr.isEmpty()) {
            throw new IllegalArgumentException("Fecha vacía o nula en el registro: " + record.getRecordNumber());
        }
        LocalDateTime fecha = LocalDateTime.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        int duracion = parseIntOrDefault(record.get("Duración"), 0);

        String causaTerminacionStr = record.get("Causa Terminación");
        int causaTerminacion = identificarTipoTerminacion(causaTerminacionStr);

        String tipoOrigenStr = record.get("Origen Corte");
        int tipoOrigen = identificarTipoOrigen(tipoOrigenStr);

        String tipoTipificacionStr = record.get("Tipificación");
        int tipoTipificacion = identificarTipoTipificacion(tipoTipificacionStr);

        // Creación del DTO
        LlamadasDTO llamadasDTO = new LlamadasDTO();
        llamadasDTO.setNumero(numero);
        llamadasDTO.setFecha(fecha);
        llamadasDTO.setDuracion(duracion);
        llamadasDTO.setCausaTerminacion(causaTerminacion);
        llamadasDTO.setTipoOrigen(tipoOrigen);
        llamadasDTO.setTipoTipificacion(tipoTipificacion);

        return llamadasDTO;
    }


    private Llamadas convertDtoaEntidad(LlamadasDTO llamadasDTO) {
        Llamadas llamadas = new Llamadas();

        // Buscar y asignar el cliente relacionado
        Cliente cliente = clienteRepository.findById(llamadasDTO.getNumero())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con número: " + llamadasDTO.getNumero()));
        llamadas.setCliente(cliente);

        // Buscar y asignar el tipo de tipificación
        TipoTipificacion tipificacion = tipoTipificacionRepository.findById(llamadasDTO.getTipoTipificacion())
                .orElseThrow(() -> new IllegalArgumentException("Tipificación no encontrada con ID: " + llamadasDTO.getTipoTipificacion()));
        llamadas.setTipoTipificacion(tipificacion);

        // Buscar y asignar el tipo de origen
        TipoOrigen tipoOrigen = tipoOrigenRepository.findById(llamadasDTO.getTipoOrigen())
                .orElseThrow(() -> new IllegalArgumentException("Origen no encontrado con ID: " + llamadasDTO.getTipoOrigen()));
        llamadas.setTipoOrigen(tipoOrigen);

        // Buscar y asignar la causa de terminación
        CausaTerminacion causaTerminacion = causaTerminacionRepository.findById(llamadasDTO.getCausaTerminacion())
                .orElseThrow(() -> new IllegalArgumentException("Causa de terminación no encontrada con ID: " + llamadasDTO.getCausaTerminacion()));
        llamadas.setCausaTerminacion(causaTerminacion);

        // Asignar atributos directos
        llamadas.setFecha(Timestamp.valueOf(llamadasDTO.getFecha())); // Convertir LocalDateTime a Timestamp
        llamadas.setDuracion(llamadasDTO.getDuracion());

        return llamadas;
    }



}
