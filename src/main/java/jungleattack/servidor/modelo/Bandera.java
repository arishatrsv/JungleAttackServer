package jungleattack.servidor.modelo;

import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.partidas.Jugador;

//Bandera de un jugador, colocada en el centro de la zona donde empieza su manada.
//El jugador que la toque con un animal del oponente gana la partida.
public class Bandera {
    private final Jugador propietario;
    private final Zona zona;
    private final Punto posicion;

    public Bandera(Jugador propietario, Zona zona) {
        this.propietario = propietario;
        this.zona = zona;
        this.posicion = new Punto(zona.consultaPosicion().consultaPosicionX(), zona.consultaPosicion().consultaPosicionY());
    }

    public Jugador consultaPropietario() {
        return propietario;
    }

    public Zona consultaZona() {
        return zona;
    }

    public Punto consultaPosicion() {
        return posicion;
    }

    public boolean deteccionContacto(Animal animal) {
        return false;
    }
}
