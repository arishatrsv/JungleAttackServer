package jungleattack.servidor.protocolo;

import com.google.gson.Gson; // Para convertir entre JSON y objetos Java.
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//Convertir entre Mensaje y JSON.
public class ConversorMensajes {
    // Un solo objeto Gson para toda la clase.
    private static final Gson GSON = new Gson();

    // Constructor privado
    private ConversorMensajes() {
    }
    //@throws IllegalArgumentException si el texto no es un JSON válido o no trae el tipo
    public static Mensaje conversionDesdeJson(String json) {
        try {
            Mensaje leido = GSON.fromJson(json, Mensaje.class);
            if (leido == null || leido.consultaTipo() == null) {
                throw new IllegalArgumentException("El mensaje no tiene tipo");
            }
            // Si no trae contenido lo dejo como un objeto vacío para que después no haya null.
            String contenido = leido.consultaContenido() == null ? "{}" : leido.consultaContenido();
            return new Mensaje(leido.consultaTipo(), contenido);
        } catch (RuntimeException e) {
            // JsonSyntaxException y cualquier otro error de formato los convierto en uno solo.
            throw new IllegalArgumentException("Mensaje con formato inválido", e);
        }
    }

    // Convierte un Mensaje en una línea de texto JSON lista para enviar por el socket. 
    public static String conversionAJson(Mensaje mensaje) {
        return GSON.toJson(mensaje);
    }

    // Crea un mensaje con el tipo y los campos indicados.
    // @param camposYValores lista de pares: campo1, valor1, campo2, valor2...
    public static Mensaje creacionMensaje(String tipo, String... camposYValores) {
        if (camposYValores.length % 2 != 0) {
            throw new IllegalArgumentException("Cada campo necesita su valor");
        }
        JsonObject contenido = new JsonObject();
        for (int i = 0; i < camposYValores.length; i += 2) {
            contenido.addProperty(camposYValores[i], camposYValores[i + 1]);
        }
        return new Mensaje(tipo, contenido.toString());
    }

    // Devuelve el valor de un campo del contenido del mensaje, o null si no existe.
    public static String campoDelContenido(Mensaje mensaje, String campo) {
        try {
            JsonObject contenido = JsonParser.parseString(mensaje.consultaContenido()).getAsJsonObject();
            JsonElement valor = contenido.get(campo);
            return (valor == null || valor.isJsonNull()) ? null : valor.getAsString();
        } catch (RuntimeException e) {
            return null; // contenido mal formado
        }
    }
}
