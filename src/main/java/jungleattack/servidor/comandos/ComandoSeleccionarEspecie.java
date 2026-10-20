package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import jungleattack.servidor.partidas.EstadoPartida;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

//Comando del cliente que elige su especie para jugar.
public class ComandoSeleccionarEspecie implements ComandoCliente {
    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        SesionPartida sesion = contexto.consultaSesion();
        Jugador jugador = contexto.consultaJugador();

        // Solo se puede elegir cuando estamos en la selección de especie.
        if (sesion == null || sesion.consultaEstado() != EstadoPartida.SELECCION_ESPECIE) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
            return;
        }

        // Cada jugador elige una sola vez.
        if (jugador.consultaEspecie() != null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_ESPECIE_YA_ELEGIDA);
            return;
        }

        // La especie tiene que existir.
        String identificador = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_ESPECIE);
        Especie especie = Especie.consultaPorIdentificador(identificador);
        if (especie == null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_ESPECIE_INVALIDA);
            return;
        }

        // eleccionEspecie decide si sigue libre 
        // (y es segura entre hilos: si los dos piden la misma a la vez, solo uno la consigue).
        if (!sesion.eleccionEspecie(jugador, especie)) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_ESPECIE_NO_DISPONIBLE);
            return;
        }
        // Aviso a los dos jugadores cómo quedó la selección.
        sesion.actualizacionEstado();
        sesion.inicio(); //Si ya eligieron los dos,empieza la partida
    }
    private void enviarError(ContextoCliente contexto, String motivo) {
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
    }
}
