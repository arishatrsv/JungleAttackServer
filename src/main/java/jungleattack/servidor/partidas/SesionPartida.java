package jungleattack.servidor.partidas;

import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.protocolo.ConversorMensajes;
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
    //Synchronized para que no se agreguen dos jugadores a la vez y se pase de 2.
    public synchronized boolean registroJugador(Jugador jugador) {
        if (estado != EstadoPartida.ESPERANDO_JUGADOR
                || jugadores.size() >= ConstantesJuego.CANTIDAD_JUGADORES) {
            return false; //La partida ya está llena o ya no esta en espera de jugadores.
        }
        jugadores.add(jugador);
        if (jugadores.size() == ConstantesJuego.CANTIDAD_JUGADORES) {
            estado = EstadoPartida.SELECCION_ESPECIE; //Ya hay dos jugadores, se pasa a la fase de selección de especie.
        }
        return true;
    }

    //Guarda la especie que escoge un jugador.
    //Si los dos jugadores eligen el mismo animal, solo el primero se lo queda.
    public synchronized boolean eleccionEspecie(Jugador jugador, Especie especie) {
        if (estado != EstadoPartida.SELECCION_ESPECIE || !especiesDisponibles().contains(especie)) {
            return false; //Ya la especie la tiene otro jugador
        }
        jugador.seleccionEspecie(especie); //Guarda la especie elegida por el jugador
        return true;
    }

    // Especies que todavía nadie ha elegido
    public List<Especie> especiesDisponibles() {
        List<Especie> disponibles = new ArrayList<>(List.of(Especie.values()));
        for (Jugador jugador : jugadores) {
            disponibles.remove(jugador.consultaEspecie()); // si es null no quita nada
        }
        return disponibles;
    }

    // Dice si los dos jugadores ya eligieron su especie.
    public boolean seleccionCompleta() {
        if (jugadores.size() < ConstantesJuego.CANTIDAD_JUGADORES) { //Todavía no hay dos jugadores
            return false;
        }
        for (Jugador jugador : jugadores) { 
            if (jugador.consultaEspecie() == null) { //todavía no eligió
                return false;
            }
        }
        return true; // los dos eligieron su especie
    }

    //Cancela la partida y avisa a los jugadores conectados.
    public synchronized void cancelacion(String motivo) {
        estado = EstadoPartida.FINALIZADA;
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConectado()) {
                // Primero lo saca de la partida y DESPUÉS le avisa. 
                jugador.salidaDePartida();
                jugador.envioMensaje(ConversorMensajes.creacionMensaje(
                        ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
            }
        }
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

    //Envia a cada jugador el estado de la partida, con las especies elegidas y las disponibles.
    public void actualizacionEstado() {
        if (estado != EstadoPartida.SELECCION_ESPECIE) {
            return; //Solo envia el estado de selección de especie
        }
        String disponibles = unionIdentificadores(especiesDisponibles());
        String completa = Boolean.toString(seleccionCompleta());
        for (Jugador jugador : jugadores) {
            Jugador oponente = oponenteDe(jugador);
            jugador.envioMensaje(ConversorMensajes.creacionMensaje( //Envía a cada jugador el estado de la partida
                    ConstantesProtocolo.SELECCION_ANIMALES,
                    ConstantesProtocolo.CAMPO_DISPONIBLES, disponibles,
                    ConstantesProtocolo.CAMPO_PROPIA, identificadorDe(jugador.consultaEspecie()),
                    ConstantesProtocolo.CAMPO_OPONENTE, identificadorDe(oponente == null ? null : oponente.consultaEspecie()),
                    ConstantesProtocolo.CAMPO_COMPLETA, completa));
        }
    }

    //Busca el jugador que corresponde a una conexión, o null si no está en esta partida.
    public Jugador jugadorPorConexion(ManejadorCliente conexion) {
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConexion() == conexion) {
                return jugador;
            }
        }
        return null;
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

    // Devuelve el otro jugador de la partida, o null si todavía no hay otro.
    private Jugador oponenteDe(Jugador jugador) {
        for (Jugador otro : jugadores) { //Busca al otro jugador
            if (otro != jugador) { //Si es distinto del jugador que se pasa como parámetro, es el oponente
                return otro;
            }
        }
        return null;
    }

    // Identificador de una especie
    private String identificadorDe(Especie especie) {
        return especie == null ? "" : especie.consultaIdentificador(); 
    }

    //Une los identificadores de una lista de especies en un solo texto separado por comas.
    private String unionIdentificadores(List<Especie> especies) {
        List<String> identificadores = new ArrayList<>();
        for (Especie especie : especies) {
            identificadores.add(especie.consultaIdentificador());
        }
        return String.join(ConstantesProtocolo.SEPARADOR_LISTA, identificadores);
    }

}
