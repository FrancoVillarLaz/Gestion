package WC.gestion.util;

import java.util.Map;

public class MapUtil {
    private static final Map<String,Integer> ORIGEN_CORTE = Map.ofEntries(

            Map.entry("Cliente", 1),
            Map.entry("Agente", 2)
    );

    private static final Map<String,Integer> CONTRATO = Map.ofEntries(
            Map.entry("Contrato CPP", 1),
            Map.entry("Activa (Prepago)", 2)
    );

    private static final Map<String, Integer> TRAMITES = Map.ofEntries(
            Map.entry("PortOut", 1),
            Map.entry("PortIn", 2),
            Map.entry("Unknow", 3)
    );

    private static final Map<String, Integer> COMPANIAS = Map.ofEntries(
            Map.entry("Claro", 1),
            Map.entry("Imowi", 2),
            Map.entry("Telecom Personal", 3),
            Map.entry("Claro (Fija)", 4),
            Map.entry("Citilan", 5),
            Map.entry("Telmex", 6),
            Map.entry("Telecentro", 7),
            Map.entry("Centurylink", 8),
            Map.entry("Idt Argentina", 9),
            Map.entry("Metrotel", 10),
            Map.entry("Llamada Ip", 11),
            Map.entry("Bvnet", 12),
            Map.entry("Convergia", 13),
            Map.entry("Internet winds", 14),
            Map.entry("Cptel", 15),
            Map.entry("Iplan", 16)
    );

    private static final Map<String, Integer> CAUSAS_TERMINACION = Map.ofEntries(
            Map.entry("Abandona en cola antes de hablar con un operador", 1),
            Map.entry("Atiende un contestador", 2),
            Map.entry("Causa Normal no especificada",3),
            Map.entry("Congestion",4),
            Map.entry("Corte Normal",5),
            Map.entry("El Cliente Atiende y Corta durante Deteccion de Voz Viva",6),
            Map.entry("El Cliente Atiende y No Habla",7),
            Map.entry("El Destino está Ocupado",8),
            Map.entry("La Linea se encuentra en reparación",9),
            Map.entry("Llamada no atendida por parte del abonado B",10),
            Map.entry("Se contacta con el operador",11),
            Map.entry("Se discó un número que no corresponde a un abonado en servicio", 12)
    );
    private static final Map<String, Integer> TIPO_TIPIFICACION = Map.ofEntries(
            Map.entry("Venta", 1),
            Map.entry("No Disp.", 2),
            Map.entry("Cliente Ocupado",3),
            Map.entry("Número equivocado",4),
            Map.entry("Mala experiencia anterior con MVS",5),
            Map.entry("Agendado por el Vdor",6),
            Map.entry("Se Corto Llamada",7),
            Map.entry("Edificio sin Disp de Caja",8),
            Map.entry("Problemas de señal MVS en su zona",9),
            Map.entry("No quiere volver a ser contactado",10),
            Map.entry("Contestador",11),
            Map.entry("Conforme con plan-prestador actual", 12),
            Map.entry("Cliente Corporativo", 13),
            Map.entry("Ya tiene MVS", 14),
            Map.entry("Cliente moroso (Supera umbral)", 15),
            Map.entry("No usa o No tiene celular", 16)

    );


    private static <T> T validarEntrada(Map<String, T> mapa, String clave, String mensajeError) {
        if (clave == null || !mapa.containsKey(clave)) {
            throw new IllegalArgumentException(mensajeError + clave);
        }
        return mapa.get(clave);
    }


    public static Integer identificarTipoContrato(String tipoContrato){
        return validarEntrada(CONTRATO,tipoContrato,"Tipo de contrano no reconocido: ");
    }

    public static Integer identificarTipoTramite(String tipoTramite){
        return validarEntrada(TRAMITES,tipoTramite,"Tramite no reconocido: ");
    }

    public static Integer identificarTipoCompania(String compania) {
        return validarEntrada(COMPANIAS,compania,"Compañía no reconocida: ");
    }

    public static Integer identificarTipoTerminacion(String terminacion){
        return validarEntrada(CAUSAS_TERMINACION,terminacion,"Causa de terminacion no reconocida: ");
    }

    public static Integer identificarTipoOrigen (String origen){
        return validarEntrada(ORIGEN_CORTE,origen,"Tipo origen de corte no reconocido: ");
    }
    public static Integer identificarTipoTipificacion (String tipificacion){
        return validarEntrada(TIPO_TIPIFICACION,tipificacion,"Tipificacion no reconocida: ");
    }
}
