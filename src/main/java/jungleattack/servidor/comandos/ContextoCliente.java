package jungleattack.servidor.comandos;

import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;
import jungleattack.servidor.red.ManejadorCliente;

//Guarda la información de un cliente que se conecta al servidor(su avatar, la partida en la que está y la conexión)
//Hay un contexto por cada conexión de cliente. Por eso no se mezclan datos de clientes.

public class ContextoCliente {

    private Jugador jugador; //Jugador del cliente. Es null hasta que se cree o se una a una partida

    private SesionPartida sesion; //Partida en la que está el cliente. También es null hasta que entre a una partida

    private ManejadorCliente conexion; //Conexión del cliente. Se usa para enviar mensajes al cliente.

    public ContextoCliente(ManejadorCliente conexion) {
        this.conexion = conexion;
    }

    //Guarda el jugador y la partida del cliente cuando entra a una partida
    public void actualizacionContexto(Jugador jugador, SesionPartida sesion) {
        this.jugador = jugador;
        this.sesion = sesion;
    }

    //Envía un mensaje al cliente
    public void envioMensaje(Mensaje mensaje) {
        conexion.envioMensaje(mensaje);
    }

    public Jugador consultaJugador() {
        return jugador;
    }

    public SesionPartida consultaSesion() {
        return sesion;
    }

    public ManejadorCliente consultaConexion() {
        return conexion;
    }
}
