package jungleattack.servidor.modelo;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

//Manada de un jugador: 10 animales con 20 de energía y daño 2.
public class ManadaJugador extends Manada {

    public ManadaJugador(Especie especie) {
        super(especie, ConstantesJuego.DANIO_JUGADOR, ConstantesJuego.ENERGIA_MANADA_JUGADOR);
    }

    @Override
    protected Animal creacionAnimal(String identificador, Punto posicion) {
        return new AnimalJugador(identificador, consultaEspecie(), consultaEnergiaInicial(), posicion);
    }

    public void consumoTaza() {
    }

    public void recuperacionEnergia() {
    }

    // Devuelve los animales vivos de esta manada que el jugador seleccionó, a partir de sus identificadores.
    public List<Animal> animalesSeleccionados(List<String> identificadores) {
        List<Animal> seleccionados = new ArrayList<>();
        for (String identificador : identificadores) {
            Animal animal = animalPorIdentificador(identificador);
            if (animal != null && animal.consultaVida() && !seleccionados.contains(animal)) {
                seleccionados.add(animal);
            }
        }
        return seleccionados;
    }
}
