package jungleattack.servidor.partidas;

import java.util.List;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.red.ManejadorCliente;
import jungleattack.servidor.reglas.MotorJuego;

public class SesionPartida {

    private String codigo;

    private List<Jugador> jugadores;

    private EstadoPartida estado;

    private Mapa mapa;

    private List<Manada> manadas;

    private List<Taza> tazas;

    private List<Bandera> banderas;

    private ResumenPartida resumen;

    private MotorJuego motorJuego;

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
