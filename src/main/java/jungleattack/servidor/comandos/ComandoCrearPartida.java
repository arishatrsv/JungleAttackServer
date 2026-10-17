package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

// Comando que crea una partida nueva y asocia al cliente que lo envía como primer jugador.

public class ComandoCrearPartida implements ComandoCliente {
    // El gestor es donde viven todas las partidas del servidor.
    private final GestorPartidas gestorPartidas;
    public ComandoCrearPartida(GestorPartidas gestorPartidas) {
        this.gestorPartidas = gestorPartidas;
    }

    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        // Un cliente no puede crear una segunda partida si ya está en una.
        if (contexto.consultaSesion() != null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_YA_EN_PARTIDA);
            return;
        }

        // Valida que el apodo no este vacío y  que no se pase del largo máximo.
        String apodo = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_APODO);
        if (apodo == null || apodo.isBlank() || apodo.trim().length() > ConstantesJuego.LONGITUD_MAXIMA_APODO) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_APODO_INVALIDO);
            return;
        }

        // Pide la partida al gestor. Devuelve null si ya se llegó al máximo de partidas.
        SesionPartida sesion = gestorPartidas.creacionPartida();
        if (sesion == null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_SERVIDOR_LLENO);
            return;
        }
        // El cliente que crea la partida es el primer jugador.
        Jugador jugador = new Jugador(apodo.trim());
        sesion.registroJugador(jugador);
        contexto.actualizacionContexto(jugador, sesion); // El cliente queda asociado al jugador y a la partida
        
        // Envía mensaje para que comparta el código de la partida.
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.PARTIDA_CREADA,
                ConstantesProtocolo.CAMPO_CODIGO, sesion.consultaCodigo()));
            }

    // Envía un mensaje de error con el motivo.
    private void enviarError(ContextoCliente contexto, String motivo) {
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
    }
}
