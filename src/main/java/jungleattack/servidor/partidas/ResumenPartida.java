package jungleattack.servidor.partidas;

import common.model.Especie;
import java.util.LinkedHashMap;
import java.util.Map;

//Resultado de una partida terminada
public class ResumenPartida {
    private final Jugador ganador;
    private final String razon; // ConstantesProtocolo.RAZON_BANDERA o RAZON_ABANDONO
    private final double duracion; // segundos
    private final Map<Jugador, Estadisticas> estadisticas = new LinkedHashMap<>();

    //Lo que hizo un jugador durante la partida
    public static class Estadisticas{
        private final String apodo;
        private final Especie especie;
        private final int botsEliminados;
        private final int animalesPerdidos; // animales de su manada que murieron
        private final int tazasConsumidas; 

        public Estadisticas(String apodo, Especie especie, int botsEliminados, int animalesPerdidos, int tazasConsumidas) {
            this.apodo = apodo;
            this.especie = especie;
            this.botsEliminados = botsEliminados;
            this.animalesPerdidos = animalesPerdidos;
            this.tazasConsumidas = tazasConsumidas;
        }

        public String consultaApodo() {
            return apodo;
        }

        public Especie consultaEspecie() {
            return especie;
        }

        public int consultaBotsEliminados() {
            return botsEliminados;
        }

        public int consultaAnimalesPerdidos() {
            return animalesPerdidos;
        }

        public int consultaTazasConsumidas() {
            return tazasConsumidas;
        }
    }

    public ResumenPartida(Jugador ganador, String razon, double duracion, Map<Jugador, Estadisticas> estadisticas){
        this ganador = ganador;
        this razon = razon;
        this.duracion = duracion;
        this.estadisticas.putAll(estadisticas);
    }

    public Jugador consultaGanador() {
        return ganador;
    }

    public String consultaRazon() {
        return razon;
    }

    //Cuánto duró la partida, en segundos.
    public double consultaDuracion() {
        return duracion;
    }

    // Las estadísticas de un jugador de la partida.
    public Estadisticas consultaEstadisticas(Jugador jugador) {
        return estadisticas.get(jugador);
    }
}
