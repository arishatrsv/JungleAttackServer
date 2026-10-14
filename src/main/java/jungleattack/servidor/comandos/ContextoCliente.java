package jungleattack.servidor.comandos;

import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;
import jungleattack.servidor.red.ManejadorCliente;

public class ContextoCliente {

    private Jugador jugador;

    private SesionPartida sesion;

    private ManejadorCliente conexion;

    public void actualizacionContexto() {
        // TODO: implementar
    }

    public void envioMensaje(Mensaje mensaje) {
        // TODO: implementar
    }
}
