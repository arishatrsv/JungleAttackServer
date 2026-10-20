package jungleattack.servidor.constantes;

//Constantes de las reglas de la partida. 

public class ConstantesJuego {
    public static final int ENERGIA_MANADA_JUGADOR = 20; //Energía inicial de cada animal
    public static final int ENERGIA_ANIMAL = 10; //energía inicial de cada bot
    public static final int CANTIDAD_ANIMALES_JUGADOR = 4;
    public static final int CANTIDAD_ANIMALES_BOTS = 4;
    public static final int DANIO_JUGADOR = 2;
    public static final int DANIO_BOTS = 1;
    public static final int CANTIDAD_TAZAS = 10;
    public static final int ATTACK_COOLDOWN_MS = 1000;
    public static final int LONGITUD_CODIGO_PARTIDA = 4; //Código de las partidas con 4 caracteres alfanuméricos
    public static final String CARACTERES_CODIGO_PARTIDA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; // Solo mayúsculas y números. 
    public static final int LONGITUD_MAXIMA_APODO = 20; //Largo máximo del apodo
    public static final int CANTIDAD_JUGADORES = 2; 

    //Cómo se colocan los animales y las tazas al empezar la partida
    public static final int RADIO_FORMACION = 30; //los animales de una manada se reparten en círculo alrededor del centro de su zona
    public static final int DISTANCIA_MINIMA_TAZA_ZONA = 40; //Distancia mínima entre una taza y el centro de las dos zonas que une su camino
    public static final String SEPARADOR_IDENTIFICADOR = "-"; // jaguar-1, jaguar-2... taza-1, taza-2..
    public static final String PREFIJO_TAZA = "taza";

    // Tiempos del motor del juego
    public static final int INTERVALO_TICK_MS = 50; // cada cuánto actualiza el hilo de una zona a sus animales
    public static final int INTERVALO_DIFUSION_MS = 100; // cada cuánto se revisa si hay que enviar STATE_UPDATE a los clientes
    public static final String PREFIJO_HILO_ZONA = "hilo-zona-";
    public static final String NOMBRE_HILO_DIFUSION = "difusion-partida-";
}
