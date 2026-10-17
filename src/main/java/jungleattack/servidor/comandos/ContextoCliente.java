package jungleattack.servidor.comandos;

import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;
import jungleattack.servidor.red.ManejadorCliente;

//Guarda la información de un cliente que se conecta al servidor(su avatar, la partida en la que está y la conexión)
//Hay un contexto por cada conexión de cliente. Por eso no se mezclan datos de clientes.

public class ContextoCliente {
    //Jugador del cliente. Es null hasta que se cree o se una a una partida
    // Es volatile porque otro hilo (el del otro jugador) puede cambiarlo si se cancela la partida.
    private volatile Jugador jugador;

    // Partida en la que está este cliente. También es null hasta que entre a una.
    private volatile SesionPartida sesion;

    // Conexión de este cliente: por aquí se envian los mensajes.
    private final ManejadorCliente conexion;

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
