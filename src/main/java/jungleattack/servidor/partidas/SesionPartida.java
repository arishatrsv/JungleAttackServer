package jungleattack.servidor.partidas;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.red.ManejadorCliente;
import jungleattack.servidor.reglas.MotorJuego;

// Representa una partida en curso, con sus jugadores, estado y elementos del juego.
public class SesionPartida {

    // Código con el que el otro jugador se une a la partida.
    private final String codigo;

    // Lista de jugadores que participan en la partida. Se agregan a medida que se unen.
    private final List<Jugador> jugadores = new CopyOnWriteArrayList<>();

    // volatile porque lo cambian y lo leen hilos distintos.
    private volatile EstadoPartida estado;

    private Mapa mapa;

    private List<Manada> manadas;

    private List<Taza> tazas;

    // Una bandera por jugador gana quien toque la del oponente.
    private List<Bandera> banderas;

    private ResumenPartida resumen;

    private MotorJuego motorJuego;

    // Crea la partida en estado de espera, hasta que llegue el segundo jugador.
    public SesionPartida(String codigo) {
        this.codigo = codigo;
        this.estado = EstadoPartida.ESPERANDO_JUGADOR;
    }

    //Agrega un jugador a la partida.
    public void registroJugador(Jugador jugador) {
        jugadores.add(jugador);
    }

    //Cuenta los jugadores que siguen conectados.
    public int cantidadJugadoresConectados() {
        int conectados = 0;
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConectado()) {
                conectados++;
            }
        }
        return conectados;
    }

    public String consultaCodigo() {
        return codigo;
    }

    public EstadoPartida consultaEstado() {
        return estado;
    }

    public List<Jugador> consultaJugadores() {
        return List.copyOf(jugadores); // copia de solo lectura
    }

    public void inicio() {
        // TODO: implementar
    }

    public void actualizacion() {
        // TODO: implementar
    }

    public void finalizacion() {
        // TODO: implementar
    }

    public Jugador jugadorPorConexion(ManejadorCliente conexion) {
        // TODO: implementar
        return null;
    }

    public Animal animalPorIdentificador(String identificador) {
        // TODO: implementar
        return null;
    }

    public List<Animal> animalesEnZona(Zona zona) {
        // TODO: implementar
        return null;
    }

    public Manada manadaPorJugador(Jugador jugador) {
        // TODO: implementar
        return null;
    }

    public void actualizacionEstado() {
        // TODO: implementar
    }
}
