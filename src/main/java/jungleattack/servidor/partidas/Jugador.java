package jungleattack.servidor.partidas;

import common.model.Especie;
import jungleattack.servidor.modelo.ManadaJugador;

//participante de la partida
public class Jugador {
    private final String apodo;

    // Especie elegida.
    private Especie especie;

    // Manada del jugador. Se crea cuando empieza la partida.
    private ManadaJugador manada;

    // volatile porque la conexión del cliente y otros hilos lo leen y lo cambian.
    private volatile boolean conectado;

    //Crea un jugador conectado, todavía sin especie ni manada. 
    public Jugador(String apodo) {
        this.apodo = apodo;
        this.conectado = true;
    }

    //Guarda la especie elegida
    public void seleccionEspecie(Especie especie) {
        this.especie = especie;
    }

    //Marca al jugador como desconectado. 
    public void desconexion() {
        this.conectado = false;
    }

    public String consultaApodo() {
        return apodo;
    }

    public Especie consultaEspecie() {
        return especie;
    }

    public boolean consultaConectado() {
        return conectado;
    }
}
