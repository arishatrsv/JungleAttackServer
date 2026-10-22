package jungleattack.servidor.modelo;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

//Manada de un jugador: 10 animales con 20 de energía y daño 2.
public class ManadaJugador extends Manada {
    // Cuántas tazas consumió el jugador (para el resumen del final y para mostrarlo en la pantalla).
    private final AtomicInteger tazasConsumidas = new AtomicInteger();
    public ManadaJugador(Especie especie) {
        super(especie, ConstantesJuego.DANIO_JUGADOR, ConstantesJuego.ENERGIA_ANIMAL_JUGADOR);
    }

    @Override
    protected Animal creacionAnimal(String identificador, Punto posicion) {
        return new AnimalJugador(identificador, consultaEspecie(), consultaEnergiaInicial(), posicion);
    }

    //Un animal de la manada consumió una taza: el daño de TODOS los animales de la manada sube y se cuenta la taza.
    //Es acumulativo dos tazas dan +2.
    public void consumoTaza() {
        incrementoDanio(ConstantesJuego.DANIO_POR_TAZA);
        tazasConsumidas.incrementAndGet();
    }

    public int consultaTazasConsumidas() {
        return tazasConsumidas.get();
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
