package jungleattack.servidor.red;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.comandos.ComandoCliente;
import jungleattack.servidor.comandos.ContextoCliente;
import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.protocolo.Mensaje;
import jungleattack.servidor.protocolo.ProcesadorMensajes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

// Maneja la conexión con un cliente: recibe y envía mensajes, 
// y ejecuta los comandos que vienen en los mensajes.
public class ManejadorCliente {

    private final Socket socket;
    private final ContextoCliente contexto;
    private final EmisorMensajes emisor;
    private final ReceptorMensajes receptor;

    // Procesa cada mensaje que llega
    private final ProcesadorMensajes procesador;
    // Semáforo con 1 permiso, funciona como un candado
    private final Semaphore semaforo = new Semaphore(1);
    // Se asegura de que el cierre de conexión se haga solo una vez
    private final AtomicBoolean cerrado = new AtomicBoolean(false);

    //Prepara la conexión con el cliente, pero no la inicia.
    public ManejadorCliente(Socket socket, GestorPartidas gestorPartidas) throws IOException {
        this.socket = socket;
        this.contexto = new ContextoCliente(this);
        this.emisor = new EmisorMensajes(new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true));
        this.procesador = new ProcesadorMensajes(gestorPartidas, contexto);
        // Cada mensaje que lea el receptor se lo paso al procesador.
        this.receptor = new ReceptorMensajes(new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)),
                procesador::procesamientoMensaje);
    }

    //Se queda leyendo los mensajes del cliente hasta que se desconecta.
    public void recepcionMensajes() {
        try {
            receptor.recepcionMensajes();
        } finally {
            cierre(); // el finally garantiza que se cierre aunque haya un error
        }
    }

    // Devuelve el contexto de este cliente (lo necesita la partida para sacarlo de ella si se cancela).
    public ContextoCliente consultaContexto() {
        return contexto;
    }

    //Envía un mensaje a este cliente.
    public void envioMensaje(Mensaje mensaje) {
        emisor.envioMensaje(mensaje);
    }

    // Ejecuta un comando de forma protegida: toma el semáforo, ejecuta y SIEMPRE lo libera.
    public void ejecucionProtegida(ComandoCliente comando, Mensaje mensaje) {
        try {
            semaforo.acquire(); // Espera su turno si otro comando de este cliente está corriendo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); //Deja el hilo en estado interrumpido para que el servidor lo cierre.
            return;
        }
        try {
            comando.ejecucion(mensaje, contexto);
        } finally {
            semaforo.release(); //libera el semaforo aunque hay error
        }
    }

    // Cierra la conexión con el cliente
    public void cierre() {
        if (!cerrado.compareAndSet(false, true)) {
            return; // ya estaba cerrada
        }
        try {
            procesador.procesamientoMensaje(new Mensaje(ConstantesProtocolo.DESCONEXION, "{}"));
        } finally {
            receptor.cierre();
            try {
                socket.close();
            } catch (IOException e) {
                // el socket ya estaba cerrado
            }
        }
    }
}
