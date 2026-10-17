package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

//Comando del cliente que quiere unirse a una partida existente.
//Se encarga de verificar que la partida exista y que haya espacio para unirse y de agregar al jugador a la partida. 
public class ComandoUnirsePartida implements ComandoCliente {
    private final GestorPartidas gestorPartidas;
    public ComandoUnirsePartida(GestorPartidas gestorPartidas) {
        this.gestorPartidas = gestorPartidas;
    }

    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        // Si el cliente ya está en una partida no puede entrar a otra.
        if (contexto.consultaSesion() != null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_YA_EN_PARTIDA);
            return;
        }

        //El apodo debe ser valido
        String apodo = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_APODO);
        if (!Jugador.apodoValido(apodo)) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_APODO_INVALIDO);
            return;
        }

        // Busca la partida por el código
        String codigo = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_CODIGO);
        SesionPartida sesion = gestorPartidas.unionPartida(codigo);
        if (sesion == null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_PARTIDA_NO_ENCONTRADA);
            return;
        }

        //RegistroJugador es la que decide si todavía hay lugar (y lo hace de forma segura entre hilos).
        Jugador jugador = new Jugador(apodo.trim(), contexto.consultaConexion()); //trim: elimina espacios en blanco
        if (!sesion.registroJugador(jugador)) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_PARTIDA_LLENA);
            return;
        }
        contexto.actualizacionContexto(jugador, sesion);

        //Primero confirma a quien se unió y después avisa a los dos que empieza la selección.
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.PARTIDA_UNIDA,
                ConstantesProtocolo.CAMPO_CODIGO, sesion.consultaCodigo()));
        sesion.actualizacionEstado();
    }
    private void enviarError(ContextoCliente contexto, String motivo) {
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
    }
}
