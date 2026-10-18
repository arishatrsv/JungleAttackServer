package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.partidas.EstadoPartida;
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
        if (sesion == null) {
            return; //El cliente nunca entró a una partida, no hay nada más que limpiar
        }

        if (sesion.cantidadJugadoresConectados() == 0) {
            gestorPartidas.eliminacionPartida(sesion.consultaCodigo());
        } else if (sesion.consultaEstado() != EstadoPartida.FINALIZADA) {
            //Si el jugador se va antes de que termine la partida, la partida se cancela y
            //el otro jugador recibe el error.
            sesion.cancelacion(ConstantesProtocolo.MOTIVO_OPONENTE_DESCONECTADO);
            gestorPartidas.eliminacionPartida(sesion.consultaCodigo());
        }
    }
}
