package WC.gestion.controller;

import WC.gestion.dto.ClienteDTO;
import WC.gestion.persistencia.entities.Cliente;
import WC.gestion.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @CrossOrigin(origins = "http://localhost:3000") // Permitir React
    @GetMapping("/listado")
    public ResponseEntity<List<Cliente>> mostrarListadoClientes() {
        try {
            List<Cliente> clientes = clienteService.obtenerClientes();
            return ResponseEntity.status(HttpStatus.OK).body(clientes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/agregar")
    public ResponseEntity<String> agregarClientes(@RequestBody List<ClienteDTO> clientesDTO) {
        try {
            clienteService.agregarClientes(clientesDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Clientes agregados exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al agregar los clientes: " + e.getMessage());
        }
    }
    @PostMapping("/subir-csv")
    public ResponseEntity<String> agregarClientesDesdeCSV(@RequestParam("file") MultipartFile file) {
        System.out.println("Endpoint /subir-csv alcanzado"); // Agregar log
        try {
            clienteService.agregarClientesDesdeCSV(file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body("Clientes agregados desde CSV exitosamente.");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage()); // Agregar log
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo CSV: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al procesar los datos: " + e.getMessage()); // Agregar log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar los datos: " + e.getMessage());
        }
    }


    @GetMapping("/filtro")
    public List<Cliente> obtenerClientes(
            @RequestParam(required = false) List<String> companias,
            @RequestParam(required = false) String contrato,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(defaultValue = "true") boolean noEsCUIT,
            @RequestParam(required = false) String tipificacion,
            @RequestParam(required = false) Date fechaPortoutInicio,
            @RequestParam(required = false) Date fechaPortoutFin,
            @RequestParam(defaultValue = "100") int cantidadMaxima
    ) {
        return clienteService.obtenerClientesFiltrados(
                companias, contrato, tipoDocumento, noEsCUIT,
                tipificacion, fechaPortoutInicio, fechaPortoutFin, cantidadMaxima
        );
    }
}
