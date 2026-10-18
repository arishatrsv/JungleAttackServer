package jungleattack.servidor.partidas;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import jungleattack.servidor.aplicacion.ConfiguracionServidor;
import jungleattack.servidor.constantes.ConstantesJuego;

//Administra todas las partidas activas del servidor.
//Como varios clientes (hilos) pueden acceder a este objeto, 
//Garantizar la sincronización de los métodos que modifican el estado de las partidas.

public class GestorPartidas {

    //Partidas activas, por el código (en mayúsculas)
    private final Map<String, SesionPartida> partidas = new ConcurrentHashMap<>();

    // Generador de números aleatorios para los códigos. 
    private final Random aleatorio;

    // Constructor normal
    public GestorPartidas() {
        this(new Random());
    }

    //Constructor para las pruebas 
    public GestorPartidas(Random aleatorio) {
        this.aleatorio = aleatorio;
    }
    //Crea una partida nueva con un código único y la guarda.
    public SesionPartida creacionPartida() {
        if (partidas.size() >= ConfiguracionServidor.MAX_PARTIDAS) {
            return null;
        }
        // Generar un código aleatorio y crear la partida.
        //si dos hilos generan el mismo código a la vez, solo uno de los dos lo logra guardar.
        while (true) {
            String codigo = generacionCodigo();
            SesionPartida sesion = new SesionPartida(codigo, aleatorio);
            if (partidas.putIfAbsent(codigo, sesion) == null) {
                return sesion;
            }
        }
    }

    //Busca la partida con ese código y agrega al segundo jugador.
    public SesionPartida unionPartida(String codigo) {
        return partidaPorCodigo(codigo);
    }

    //Elimina una partida 
    public void eliminacionPartida(String codigo) {
        if (codigo != null) {
            partidas.remove(normalizacionCodigo(codigo));
        }
    }

    //Devuelve la partida con ese código, o null si no existe. 
    public SesionPartida partidaPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        return partidas.get(normalizacionCodigo(codigo));
    }

    //Cantidad de partidas activas en este momento.
    public int cantidadPartidasActivas() {
        return partidas.size();
    }

    //Cantidad total de jugadores conectados en todas las partidas. 
    public int cantidadJugadoresConectados() {
        int total = 0;
        for (SesionPartida sesion : partidas.values()) {
            total += sesion.cantidadJugadoresConectados();
        }
        return total;
    }

    //Estado de una partida, o null si el código no existe.
    public EstadoPartida estadoPartida(String codigo) {
        SesionPartida sesion = partidaPorCodigo(codigo);
        return sesion == null ? null : sesion.consultaEstado();
    }

    //Hace un código aleatorio con los caracteres
    private String generacionCodigo() {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < ConstantesJuego.LONGITUD_CODIGO_PARTIDA; i++) {
            int posicion = aleatorio.nextInt(ConstantesJuego.CARACTERES_CODIGO_PARTIDA.length());
            codigo.append(ConstantesJuego.CARACTERES_CODIGO_PARTIDA.charAt(posicion));
        }
        return codigo.toString();
    }

    //Normaliza el código(Pasa a mayusculas y quita espacios)
    private String normalizacionCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}

