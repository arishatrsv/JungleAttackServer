package jungleattack.servidor.modelo;

import common.model.Especie;
import jungleattack.servidor.mapa.Punto;

public class AnimalBot extends Animal {
    public AnimalBot(String identificador, Especie especie, int energia, Punto posicion) {
        super(identificador, especie, energia, posicion);
    }

    @Override
    public boolean esBot() {
        return true;
    }

    public Animal seleccionObjetivo() {
        return null;
    }

    public void ataqueAutomatico() {
        // TODO: implementar
    }
}
