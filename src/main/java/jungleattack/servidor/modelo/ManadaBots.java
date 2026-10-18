package jungleattack.servidor.modelo;
import common.model.Especie;
import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

// Manada de bots: 4 animales con 10 de energía y daño 1, controlados por el servidor.
public class ManadaBots extends Manada {
    public ManadaBots(Especie especie) {
        super(especie, ConstantesJuego.DANIO_BOTS, ConstantesJuego.ENERGIA_ANIMAL);
    }

    @Override
    protected Animal creacionAnimal(String identificador, Punto posicion) {
        return new AnimalBot(identificador, consultaEspecie(), consultaEnergiaInicial(), posicion);
    }

    public void actualizacionBots() {
    }

    public List<Animal> animalesEnAlcance(Animal animal) {
        return null;
    }
}