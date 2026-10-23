package jungleattack.servidor.reglas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.ResumenPartida;

//Jugador gana cuando uno de sus animales toca la bandera del oponente,
//y la partida termina en ese instante
public class ReglasVictoria {
    private final List<Bandera> banderas;
    private final List<Jugador> jugadores;

    public ReglasVictoria(List<Bandera> banderas, List<Jugador> jugadores) {
        this.banderas = banderas;
        this.jugadores = jugadores;
    }

    //Revisa si un animal que acaba de moverse está tocando la bandera del oponente
    public Jugador comprobacionVictoria(Animal animal) {
        for (Bandera bandera : banderas) {
            if (bandera.deteccionContacto(animal)) {
                for (Jugador jugador : jugadores) {
                    if (jugador.consultaManada() == animal.consultaManada()) {
                        return jugador;
                    }
                }
            }
        }
        return null;
    }

    //Arma el resumen de la partida con las estadisticas de cada jugador
    public ResumenPartida creacionResumen(Jugador ganador, String razon, long inicio, long fin) {
        Map<Jugador, ResumenPartida.Estadisticas> estadisticas = new LinkedHashMap<>();
        for (Jugador jugador : jugadores) {
            ManadaJugador manada = jugador.consultaManada();
            estadisticas.put(jugador, new ResumenPartida.Estadisticas(jugador.consultaApodo(), jugador.consultaEspecie(),
                    manada.consultaBotsEliminados(), manada.cantidadAnimalesMuertos(), manada.consultaTazasConsumidas()));
        }
        return new ResumenPartida(ganador, razon, Math.max(0, fin - inicio) / 1000.0, estadisticas);
    }
}
