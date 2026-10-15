package jungleattack.servidor.comandos;

import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;

//Se ejecuta cuando un cliente se desconecta del servidor. 
//Se encarga de eliminar al jugador de la partida en la que estaba y 
//de eliminar la partida si ya no hay jugadores en ella.

public class ComandoDesconexion implements ComandoCliente {
    private final GestorPartidas gestorPartidas;

    public ComandoDesconexion(GestorPartidas gestorPartidas) {
        this.gestorPartidas = gestorPartidas;
    }

    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        Jugador jugador = contexto.consultaJugador();
        SesionPartida sesion = contexto.consultaSesion();

        if (jugador != null) {
            jugador.desconexion();
        }

        // Si ya no queda nadie conectado en la partida, la quito del gestor.
        if (sesion != null && sesion.cantidadJugadoresConectados() == 0) {
            gestorPartidas.eliminacionPartida(sesion.consultaCodigo());
        }

        //Falta proximo paso
    }
}
