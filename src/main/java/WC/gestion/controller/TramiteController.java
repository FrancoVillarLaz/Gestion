package WC.gestion.controller;

import WC.gestion.dto.TramiteDTO;
import WC.gestion.service.TramiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    private final TramiteService tramiteService;

    public TramiteController(TramiteService tramiteService) {
        this.tramiteService = tramiteService;
    }

    @PostMapping("/agregar")
    public ResponseEntity<String> agregarTramite(@RequestBody List<TramiteDTO> tramiteDTO ){
        try {
            tramiteService.agregarTramites(tramiteDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Tramite agregado exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al agregar los Tramites:" + e.getMessage());
        }
    }

    @PostMapping("/subir-csv")
    public ResponseEntity<String> agregarTramiteDesdeCsv(@RequestParam("file") MultipartFile file){
        System.out.println("Endpoint /subir-csv alcanzado");
        try {
            tramiteService.agregarTramitesDesdeCsv(file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body("Tramites agregados desde CSV exitosamente");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo"+ e.getMessage());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar los datos" + e.getMessage());
        }
    }
}
