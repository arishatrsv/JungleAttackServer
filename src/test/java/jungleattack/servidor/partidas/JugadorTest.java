package jungleattack.servidor.partidas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JugadorTest {

    @Test
    void unApodoNormalEsValido() {
        assertTrue(Jugador.apodoValido("Ana"));
        assertTrue(Jugador.apodoValido("  Ana  ")); // los espacios de los lados no cuentan
    }

    @Test
    void elApodoVacioONuloNoEsValido() {
        assertFalse(Jugador.apodoValido(null));
        assertFalse(Jugador.apodoValido(""));
        assertFalse(Jugador.apodoValido("   "));
    }

    @Test
    void unApodoMuyLargoNoEsValido() {
        assertFalse(Jugador.apodoValido("a".repeat(100)));
    }

    @Test
    void unJugadorNuevoEstaConectadoYSinEspecie() {
        Jugador jugador = new Jugador("Ana", null);
        assertTrue(jugador.consultaConectado());
        assertFalse(jugador.consultaEspecie() != null);
    }
}
