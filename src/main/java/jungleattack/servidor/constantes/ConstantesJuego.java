package jungleattack.servidor.constantes;

/**
 * Constantes de las reglas. Valores del enunciado: 10 tazas, ataque cada 1000 ms, daño 2 (jugador) y 1 (bots), energía 20 (jugador) y 10 (bots). CANTIDAD_ANIMALES_* es provisional. ENCOUNTER_RADIUS está en common.constantes.ConstantesComunes.
 */
public class ConstantesJuego {

    public static final int ENERGIA_MANADA_JUGADOR = 20;

    public static final int ENERGIA_ANIMAL = 10;

    public static final int CANTIDAD_ANIMALES_JUGADOR = 4;

    public static final int CANTIDAD_ANIMALES_BOTS = 4;

    public static final int DANIO_JUGADOR = 2;

    public static final int DANIO_BOTS = 1;

    public static final int CANTIDAD_TAZAS = 10;

    public static final int ATTACK_COOLDOWN_MS = 1000;
}
