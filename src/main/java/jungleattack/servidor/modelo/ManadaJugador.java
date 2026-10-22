package jungleattack.servidor.modelo;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import common.constantes.ConstantesComunes;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

//Manada de un jugador: 10 animales con 20 de energía y daño 2.
public class ManadaJugador extends Manada {
    //Cuántas tazas consumió el jugador (para el resumen del final y para mostrarlo en la pantalla).
    private final AtomicInteger tazasConsumidas = new AtomicInteger();
    //Cuántos bots eliminaron los animales de esta manada.
    private final AtomicInteger botsEliminados = new AtomicInteger();

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

    //Anota que un animal de la manada eliminó a un bot
    public int registroBotEliminado() {
        return botsEliminados.incrementAndGet();
    }

    public int consultaBotsEliminados() {
        return botsEliminados.get();
    }

    //cada animal VIVO de la manada gana energía por eliminar bots
    public void recuperacionEnergia() {
        for (Animal animal : consultaAnimales()) {
            synchronized (animal) { // otro hilo podría estar hiriendo a este animal justo ahora
                if (animal.consultaVida()) {
                    animal.actualizacionEnergia(Math.min(ConstantesComunes.MAX_PLAYER_ENERGY,
                            animal.consultaEnergia() + ConstantesJuego.ENERGIA_POR_ELIMINACIONES));
                }
            }
        }
    }

    //Devuelve los animales vivos de esta manada que el jugador seleccionó, a partir de sus identificadores.
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
