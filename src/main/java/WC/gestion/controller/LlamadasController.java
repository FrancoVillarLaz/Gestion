package WC.gestion.controller;

import WC.gestion.service.LlamadasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/llamadas")
public class LlamadasController {

    private final LlamadasService llamadasService;

    public LlamadasController(LlamadasService llamadasService) {
        this.llamadasService = llamadasService;
    }

    @PostMapping("/subir-csv")
    public ResponseEntity<String> agregarClientesDesdeCSV(@RequestParam("file") MultipartFile file) {
        System.out.println("Endpoint /subir-csv alcanzado"); // Agregar log
        try {
            llamadasService.agregarLlamadaDesdeCsv(file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body("Clientes agregados desde CSV exitosamente.");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage()); // Agregar log
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo CSV: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al procesar los datos: " + e.getMessage()); // Agregar log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar los datos: " + e.getMessage());
        }
    }

}
