package jungleattack.servidor.modelo;

import jungleattack.servidor.mapa.Punto;

//Taza de café ubicada sobre un camino. La consume el primer animal que la toque.
public class Taza {

    private final String identificador;
    private final Punto posicion;
    private volatile boolean consumida;

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
        return consumida;
    }

    public void consumo() {
        this.consumida = true;
    }
}
