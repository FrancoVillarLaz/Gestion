package WC.gestion.service;

import WC.gestion.dto.ClienteDTO;
import WC.gestion.persistencia.entities.Cliente;
import WC.gestion.persistencia.entities.TipoDocumento;
import WC.gestion.persistencia.specification.ClienteSpecification;
import WC.gestion.repositories.ClienteRepository;
import WC.gestion.repositories.TipoDocumentoRepository;
import WC.gestion.util.MapUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static WC.gestion.util.DocumentoUtil.identificarTipoDocumento;
import static WC.gestion.util.NumberUtil.parseLongOrDefault;


@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;


    public Cliente agregarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> obtenerClientes(){
        return clienteRepository.findAll();
    }

    public List<Cliente> filtrarClientes(
            List<String> companias, String contrato, String tipoDocumento, boolean noEsCUIT,
            String tipificacion, Timestamp fechaLlamadaInicio, Timestamp fechaLlamadaFin,
            Integer duracionMinima, Integer duracionMaxima, String causaTerminacion) {

        // Convertir nombres a IDs utilizando MapUtil
        List<Integer> companiasIds = null;
        if (companias != null) {
            companiasIds = companias.stream()
                    .map(MapUtil::identificarTipoCompania)
                    .toList();
        }

        Integer contratoId = contrato != null ? MapUtil.identificarTipoContrato(contrato) : null;
        Integer tipificacionId = tipificacion != null ? MapUtil.identificarTipoTipificacion(tipificacion) : null;
        Integer causaTerminacionId = causaTerminacion != null ? MapUtil.identificarTipoTerminacion(causaTerminacion) : null;

        // Crear la Specification
        Specification<Cliente> spec = ClienteSpecification.getClientesEspecificacion(
                companiasIds, contratoId, tipoDocumento, noEsCUIT, tipificacionId,
                fechaLlamadaInicio, fechaLlamadaFin, duracionMinima, duracionMaxima, causaTerminacionId
        );

        // Consultar en el repositorio
        return clienteRepository.findAll(spec);
    }

    @Transactional
    public void agregarClientes(List<ClienteDTO> clientesDTO) {
        List<Cliente> clientes = clientesDTO.stream().map(this::convertirDTOaEntidad).toList();
        clienteRepository.saveAll(clientes);  // Utiliza saveAll para guardar en batch
    }

    @Transactional
    public void agregarClientesDesdeCSV(InputStream inputStream) throws IOException {
        List<String> errores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withDelimiter(';') // Separador
                     .withFirstRecordAsHeader())) {

            List<ClienteDTO> batch = new ArrayList<>();
            int batchSize = 1000; // Número de registros por lote

            for (CSVRecord record : csvParser) {
                try {
                    // Crear y validar el DTO
                    ClienteDTO clienteDTO = crearClienteDTO(record);
                    batch.add(clienteDTO);

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

    private ClienteDTO crearClienteDTO(CSVRecord record) {
        Long numero = parseLongOrDefault(record.get("numero"), null);
        String nombreCompleto = record.get("nombrecompleto").trim();
        Long documento = parseLongOrDefault(record.get("documento"), null);
        Integer tipoDocumentoId = Integer.parseInt(identificarTipoDocumento(documento.toString()));

        ClienteDTO clienteDTO = new ClienteDTO();
        clienteDTO.setNumero(numero);
        clienteDTO.setNombreCompleto(nombreCompleto);
        clienteDTO.setDocumento(documento);
        clienteDTO.setTipoDocumentoId(tipoDocumentoId);
        return clienteDTO;
    }

    private void guardarLote(List<ClienteDTO> batch) {
        List<Cliente> clientes = batch.stream()
                .map(this::convertirDTOaEntidad)
                .toList();
        clienteRepository.saveAll(clientes);
    }


    @Transactional
    public void recalcularLineas(List<Cliente> clientes) {
        Map<Long, Long> lineasPorDocumento = clienteRepository.findAll().stream()
                .collect(Collectors.groupingBy(Cliente::getDocumento, Collectors.counting()));

        for (Cliente cliente : clientes) {
            cliente.setLineas(lineasPorDocumento.getOrDefault(cliente.getDocumento(), 0L).intValue());
        }
        clienteRepository.saveAll(clientes);
    }


    private Cliente convertirDTOaEntidad(ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNumero(dto.getNumero());
        cliente.setNombreCompleto(dto.getNombreCompleto());
        cliente.setDocumento(dto.getDocumento());
        cliente.setLineas(dto.getLineas());

        // Recupera el TipoDocumento desde la base de datos
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(dto.getTipoDocumentoId())
                .orElseThrow(() -> new IllegalArgumentException("TipoDocumento no encontrado con id: " + dto.getTipoDocumentoId()));
        cliente.setTipoDocumento(tipoDocumento);

        return cliente;
    }
}
