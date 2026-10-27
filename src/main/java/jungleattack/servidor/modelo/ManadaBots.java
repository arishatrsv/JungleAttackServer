package jungleattack.servidor.modelo;
import common.model.Especie;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

// Manada de bots: 4 animales con 10 de energía y daño 1, controlados por el servidor.
public class ManadaBots extends Manada {
    public ManadaBots(Especie especie) {
        super(especie, ConstantesJuego.DANIO_BOTS, ConstantesJuego.ENERGIA_ANIMAL_BOTS);
    }

    @Override
    protected Animal creacionAnimal(String identificador, Punto posicion) {
        return new AnimalBot(identificador, consultaEspecie(), consultaEnergiaInicial(), posicion);
    }
}