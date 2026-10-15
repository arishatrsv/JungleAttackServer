package jungleattack.servidor.red;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

// Lee los mensajes que llegan del cliente por el socket 
// y los pasa a un destino
public class ReceptorMensajes {

    // Entrada del socket del cliente, para leer los mensajes que envía.
    private final BufferedReader entrada;

    // Destino de los mensajes leídos
    private final Consumer<Mensaje> destino;

    // Mientras sea true sigo leyendo
    private volatile boolean activo;

    public ReceptorMensajes(BufferedReader entrada, Consumer<Mensaje> destino) {
        this.entrada = entrada;
        this.destino = destino;
    }

    // Lee los mensajes del cliente hasta que se desconecta o se llama a cierre().
    public void recepcionMensajes() {
        activo = true;
        try {
            String linea;
            // readLine() devuelve null cuando el cliente cerró la conexión.
            while (activo && (linea = entrada.readLine()) != null) {
                if (linea.isBlank()) {
                    continue; // ignora las líneas vacías
                }
                Mensaje mensaje;
                try {
                    mensaje = ConversorMensajes.conversionDesdeJson(linea);
                } catch (IllegalArgumentException e) {
                    //Si no es JSON válido, crea un mensaje de error al cliente.
                    mensaje = new Mensaje(ConstantesProtocolo.TIPO_INVALIDO, "{}");
                }
                destino.accept(mensaje);
            }
        } catch (IOException e) {
            // El socket se cerró mientras esperaba: es la forma normal de terminar, no es un fallo.
        }
    }
    // Cierra la recepción de mensajes.
    // Si el hilo está bloqueado se desbloquea y termina.
    public void cierre() {
        activo = false;
    }
}
