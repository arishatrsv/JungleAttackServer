package jungleattack.servidor.reglas;

import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.Jugador;

public class ReglasMovimiento {

    public List<Zona> calculoRuta(Zona origen, Zona destino) {
        // TODO: implementar
        return null;
    }

    public boolean validacionMovimiento(Jugador jugador, List<Animal> animales, Punto destino) {
        // TODO: implementar
        return false;
    }

    public void inicioMovimiento(Animal animal, Punto destino) {
        // TODO: implementar
    }

    public void actualizacionPosicion(Animal animal, long deltaTiempo) {
        // TODO: implementar
    }

    public void finalizacionMovimiento(Animal animal) {
        // TODO: implementar
    }
}
