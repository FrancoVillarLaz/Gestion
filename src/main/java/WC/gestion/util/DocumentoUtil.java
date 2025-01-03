package WC.gestion.util;

public class DocumentoUtil {

    public static String identificarTipoDocumento(String documento) {
        // Regla para C.U.I.T o C.U.I.L
        if (documento.length() == 11) {
            return "2";
        }
        // Regla para Documento Nacional Identidad
        if (documento.length() <= 8 && documento.matches("^\\d+$")) {
            return "1";
        }
        // Regla para Cédula de Identidad
        if (documento.matches("^[A-Za-z0-9]+$") && documento.length() <= 12) {
            return "3";
        }
        // Regla para Libreta Cívica
        if (documento.matches("^\\d{1,7}$")) {
            return "4";
        }
        // Regla para Libreta de Enrolamiento
        if (documento.matches("^\\d{1,7}$") ) {
            return "5";
        }
        // Regla para Pasaporte
        if (documento.matches("^[A-Z]{1}\\d+$")) {
            return "6";
        }
        // Si no coincide con ninguna regla
        throw new IllegalArgumentException("Tipo de documento desconocido para: " + documento);
    }

}
