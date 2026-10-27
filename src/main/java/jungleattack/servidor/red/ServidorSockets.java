package jungleattack.servidor.red;

import jungleattack.servidor.partidas.GestorPartidas;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acepta las conexiones de los clientes. Por cada cliente nuevo crea un ManejadorCliente
 * y lo ejecuta en un hilo aparte, así el servidor puede atender a muchos clientes a la vez.
 */
public class ServidorSockets {
    private static final Logger REGISTRO = Logger.getLogger(ServidorSockets.class.getName());
    private final int puerto; // Puerto donde escucha el servidor
    private final GestorPartidas gestorPartidas; // Las partidas de todos los clientes 
    //crea hilos nuevos según se necesiten y reutiliza los que ya terminaron.
    private final ExecutorService hilos = Executors.newCachedThreadPool(); 
    private volatile boolean activo; // Mientras sea true el servidor sigue aceptando clientes
    private volatile ServerSocket servidor;

    public ServidorSockets(int puerto, GestorPartidas gestorPartidas) {
        this.puerto = puerto;
        this.gestorPartidas = gestorPartidas;
    }

    // Inicia el servidor se queda escuchando a los clientes hasta que se llama a cierre().
    public void inicio() {
        try (ServerSocket socketServidor = new ServerSocket(puerto)) {
            servidor = socketServidor;
            activo = true;
            REGISTRO.info("Servidor escuchando en el puerto " + puerto);
            while (activo) {
                Socket socketCliente = socketServidor.accept(); // espera a que llegue un cliente
                atencionCliente(socketCliente);
            }
        } catch (IOException e) {
            if (activo) { //si es false es porque se llamó a cierre() 
                REGISTRO.log(Level.SEVERE, "Error en el servidor", e);
            }
        } finally {
            activo = false;
            hilos.shutdown();
        }
    }

    // Prepara la atención de un cliente recién aceptado y la ejecuta en su propio hilo.
    // Si algo falla con ESTE cliente, solo se cierra su socket: el servidor sigue aceptando a los demás.
    private void atencionCliente(Socket socketCliente) {
        try {
            ManejadorCliente manejador = new ManejadorCliente(socketCliente, gestorPartidas);
            hilos.execute(manejador::recepcionMensajes); // cada cliente en su propio hilo
        } catch (IOException | RuntimeException e) {
            REGISTRO.log(Level.WARNING, "No se pudo atender a un cliente", e);
            try {
                socketCliente.close();
            } catch (IOException ignorado) {
                // el socket ya estaba cerrado
            }
        }
    }

    // Cierra el servidor: deja de aceptar clientes y termina los hilos.
    public void cierre() {
        activo = false;
        try {
            if (servidor != null) {
                servidor.close(); // esto hace que accept() termine con una excepción
            }
        } catch (IOException e) {
            REGISTRO.log(Level.WARNING, "No se pudo cerrar el puerto", e);
        }
    }
}

