package WC.gestion.util;

import java.io.*;
import java.util.Optional;

public class NumberUtil {

    public static  Long parseLongOrDefault(String value, Long defaultValue){
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }

    public static Integer parseIntOrDefault ( String value, Integer defaultValue){
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e){
            return defaultValue;
        }
    }


    public static String saca15(String numero) {
                // Eliminar el '0' al principio si está presente
                if (numero.startsWith("0")) {
                    numero = numero.substring(1);
                }

                // Eliminar el prefijo '90' si está presente
                if (numero.startsWith("90")) {
                    numero = numero.substring(2);
                }

                // Procesar números que empiezan con '11' (Buenos Aires)
                if (numero.startsWith("11")) {
                    if (numero.length() > 4 && numero.substring(2, 4).equals("15")) {
                        numero = "11" + numero.substring(4); // Remover "15" después del prefijo "11"
                    }
                } else {
                    // Procesar otras características (3 o 4 dígitos)
                    if (numero.length() >= 5) {
                        String caracteristica = numero.substring(0, 3);
                        if (numero.substring(3, 5).equals("15")) { // Característica de 3 dígitos
                            numero = caracteristica + numero.substring(5);
                        } else if (numero.length() > 4
                                && numero.substring(0, 4).chars().allMatch(Character::isDigit)
                                && numero.substring(4, 6).equals("15")) { // Característica de 4 dígitos
                            caracteristica = numero.substring(0, 4);
                            numero = caracteristica + numero.substring(6);
                        }
                    }
                }
                return numero;
            }
    
            
}
