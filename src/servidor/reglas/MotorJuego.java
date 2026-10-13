package jungleattack.servidor.reglas;

import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;

public class MotorJuego {

    private SesionPartida sesion;

    private List<HiloZona> hilosZona;

    private boolean activo;

    public void actualizacionJuego() {
        // TODO: implementar
    }

    public void ejecucionOrdenMovimiento(Jugador jugador, List<Animal> animales, Punto destino) {
        // TODO: implementar
    }

    public void ejecucionOrdenAtaque(Jugador jugador, List<Animal> animales, Animal objetivo) {
        // TODO: implementar
    }

    public boolean comprobacionVictoria() {
        // TODO: implementar
        return false;
    }

    public void actualizacionClientes() {
        // TODO: implementar
    }
}
