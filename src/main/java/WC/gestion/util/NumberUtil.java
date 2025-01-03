package WC.gestion.util;

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

    public static String saca15(String numeroCon15){
        if (numeroCon15.startsWith("0")){

        }
        String numero = numeroCon15;
        return numero;
    }
}
