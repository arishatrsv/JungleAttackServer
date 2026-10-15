package jungleattack.servidor.red;

import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;
import java.io.PrintWriter;

/**
 * Envía mensajes al cliente por el socket: un mensaje por línea, en formato JSON.
 */
public class EmisorMensajes {

    // Salida del socket del cliente, para enviarle mensajes.
    private final PrintWriter salida;

    public EmisorMensajes(PrintWriter salida) {
        this.salida = salida;
    }

    //Envía mensaje al cliente. Se sincroniza para que no se mezclen los mensajes si hay varios hilos enviando.
    public synchronized void envioMensaje(Mensaje mensaje) {
        salida.println(ConversorMensajes.conversionAJson(mensaje));
    }
}
