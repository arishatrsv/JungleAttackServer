package jungleattack.servidor.red;

import java.net.Socket;
import java.util.concurrent.Semaphore;
import jungleattack.servidor.comandos.ComandoCliente;
import jungleattack.servidor.comandos.ContextoCliente;
import jungleattack.servidor.protocolo.Mensaje;

public class ManejadorCliente {

    private Socket socket;

    private ContextoCliente contexto;

    private EmisorMensajes emisor;

    private ReceptorMensajes receptor;

    private Semaphore semaforo;

    public void recepcionMensajes() {
        // TODO: implementar
    }

    public void envioMensaje(Mensaje mensaje) {
        // TODO: implementar
    }

    public void ejecucionProtegida(ComandoCliente comando) {
        // TODO: implementar
    }

    public void cierre() {
        // TODO: implementar
    }
}
