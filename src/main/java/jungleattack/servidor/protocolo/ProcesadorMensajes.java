package jungleattack.servidor.protocolo;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.comandos.ComandoCliente;
import jungleattack.servidor.comandos.ComandoCrearPartida;
import jungleattack.servidor.comandos.ComandoDesconexion;
import jungleattack.servidor.comandos.ComandoMoverAnimales;
import jungleattack.servidor.comandos.ComandoSeleccionarEspecie;
import jungleattack.servidor.comandos.ComandoUnirsePartida;
import jungleattack.servidor.comandos.ContextoCliente;
import jungleattack.servidor.partidas.GestorPartidas;
import java.util.HashMap;
import java.util.Map;

// Procesa los mensajes que llegan del cliente
public class ProcesadorMensajes {

    // Mapa de tipo de mensaje
    private final Map<String, ComandoCliente> comandos = new HashMap<>();

    // Contexto del cliente de está conexión
    private final ContextoCliente contexto;

    // Registra los comandos disponibles
    public ProcesadorMensajes(GestorPartidas gestorPartidas, ContextoCliente contexto) {
        this.contexto = contexto;
        comandos.put(ConstantesProtocolo.CREAR_PARTIDA, new ComandoCrearPartida(gestorPartidas));
        comandos.put(ConstantesProtocolo.UNIRSE_PARTIDA, new ComandoUnirsePartida(gestorPartidas));
        comandos.put(ConstantesProtocolo.SELECCIONAR_ANIMAL, new ComandoSeleccionarEspecie());
        comandos.put(ConstantesProtocolo.MOVER_ANIMALES, new ComandoMoverAnimales());
        comandos.put(ConstantesProtocolo.DESCONEXION, new ComandoDesconexion(gestorPartidas));
    }

   //Procesa el mensaje recibido del cliente
    public void procesamientoMensaje(Mensaje mensaje) {
        ComandoCliente comando = comandos.get(mensaje.consultaTipo());
        if (comando == null) {
            // Distingo entre "no era un JSON válido" y "el tipo no existe" para dar un mejor error.
            String motivo = ConstantesProtocolo.TIPO_INVALIDO.equals(mensaje.consultaTipo())
                    ? ConstantesProtocolo.MOTIVO_MENSAJE_INVALIDO
                    : ConstantesProtocolo.MOTIVO_TIPO_DESCONOCIDO;
            contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                    ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
            return;
        }
        contexto.consultaConexion().ejecucionProtegida(comando, mensaje);
    }
}
