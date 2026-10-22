package jungleattack.servidor.modelo;

import java.util.concurrent.atomic.AtomicBoolean;
import jungleattack.servidor.mapa.Punto;

//Taza de café ubicada sobre un camino. La consume el primer animal que la toque.
public class Taza {

    private final String identificador;
    private final Punto posicion;
    // AtomicBoolean porque dos animales (de hilos distintos) pueden tocar la taza casi al mismo tiempo.
    private final AtomicBoolean consumida = new AtomicBoolean(false);

    public Taza(String identificador, Punto posicion) {
        this.identificador = identificador;
        this.posicion = posicion;
    }

    public String consultaIdentificador() {
        return identificador;
    }

    public Punto consultaPosicion() {
        return posicion;
    }

    public boolean consultaConsumida() {
        return consumida.get();
    }

    //Marca la taza como consumida.Solo el primero que lo intente lo logra.
    public boolean consumo() {
        return consumida.compareAndSet(false, true); //true si la consumió; false si ya estaba consumida
    }
}
