package jungleattack.servidor.modelo;

import common.model.Especie;
import jungleattack.servidor.mapa.Punto;

//Animal controlado por un jugador. Recibe órdenes de movimiento y ataque a través del servidor.
public class AnimalJugador extends Animal {

    public AnimalJugador(String identificador, Especie especie, int energia, Punto posicion) {
        super(identificador, especie, energia, posicion);
    }
}

