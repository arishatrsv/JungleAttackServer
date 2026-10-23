package jungleattack.servidor.modelo;

import jungleattack.servidor.constantes.ConstantesJuego;
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

    //Si un animal está tocando la bandera o en el radio. Solo la tocan los animales
    //vivos del equipo contrario
    public boolean deteccionContacto(Animal animal) {
        // Un bot o un animal de la manada propietaria no puede tocar esta bandera.
        if (animal.esBot() || animal.consultaManada() == propietario.consultaManada()) {
            return false;
        }
        double x;
        double y;
        synchronized (animal) { // otro hilo podría estar moviendo o hiriendo al animal
            // Un animal muerto no puede tocar la bandera.
            if (!animal.consultaVida()) {
                return false;
            }

            // Guardamos la posicion actual del animal para calcular la distancia.
            x = animal.consultaPosicion().consultaPosicionX();
            y = animal.consultaPosicion().consultaPosicionY();
        }

        // Calculamos la distancia entre el animal y la bandera:
        return Math.hypot(
                x - posicion.consultaPosicionX(), // Diferencia entre las coordenadas X.
                y - posicion.consultaPosicionY()  // Diferencia entre las coordenadas Y.
        ) <= ConstantesJuego.RADIO_BANDERA; // Hay contacto si esta dentro del radio.
    }
}
