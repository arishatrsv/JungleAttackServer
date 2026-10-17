package jungleattack.servidor.partidas;

import common.model.Especie;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.protocolo.Mensaje;
import jungleattack.servidor.red.ManejadorCliente;

//participante de la partida
public class Jugador {
    private final String apodo;
    // Conexión de este jugador. Con ella la partida le puede enviar mensajes sin pasar por el comando.
    // Puede ser null en las pruebas unitarias.
    private final ManejadorCliente conexion;
    // Especie elegida.
    private Especie especie;

    // Manada del jugador. Se crea cuando empieza la partida.
    private ManadaJugador manada;

    // volatile porque la conexión del cliente y otros hilos lo leen y lo cambian.
    private volatile boolean conectado;

    //Crea un jugador conectado, todavía sin especie ni manada. 
    public Jugador(String apodo, ManejadorCliente conexion) {
        this.apodo = apodo;
        this.conexion = conexion;
        this.conectado = true;
    }

    //Dice si el apodo se puede usar. Es estático por que se puede revisar antes de crear al jugador
    public static boolean apodoValido(String apodo) {
        return apodo != null && !apodo.isBlank()
                && apodo.trim().length() <= ConstantesJuego.LONGITUD_MAXIMA_APODO;
    }

    //Guarda la especie elegida
    public void seleccionEspecie(Especie especie) {
        this.especie = especie;
    }

    //Marca al jugador como desconectado. 
    public void desconexion() {
        this.conectado = false;
    }

    // Envía un mensaje a este jugador. 
    // Si no tiene conexión (pruebas) o ya se desconectó, no hace nada.
    public void envioMensaje(Mensaje mensaje) {
        if (conexion != null && conectado) {
            conexion.envioMensaje(mensaje);
        }
    }

    // Saca al cliente de su partida: deja su contexto sin jugador ni partida 
    // para que pueda crear o unirse a otra. Se usa cuando la partida se cancela.
    public void salidaDePartida() {
        if (conexion != null) {
            conexion.consultaContexto().actualizacionContexto(null, null);
        }
    }

    public String consultaApodo() {
        return apodo;
    }

    public Especie consultaEspecie() {
        return especie;
    }

    public boolean consultaConectado() {
        return conectado;
    }

    public ManejadorCliente consultaConexion() {
        return conexion;
    }
}
