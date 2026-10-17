package jungleattack.servidor.partidas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import jungleattack.servidor.aplicacion.ConfiguracionServidor;
import jungleattack.servidor.constantes.ConstantesJuego;
import org.junit.jupiter.api.Test;

class GestorPartidasTest {

    // Uso una semilla fija para que el Random dé siempre los mismos números y la prueba sea repetible.
    private GestorPartidas crearGestor() {
        return new GestorPartidas(new Random(42));
    }

    @Test
    void elCodigoTieneElLargoYLosCaracteresPermitidos() {
        SesionPartida sesion = crearGestor().creacionPartida();
        String codigo = sesion.consultaCodigo();
        assertEquals(ConstantesJuego.LONGITUD_CODIGO_PARTIDA, codigo.length());
        for (char c : codigo.toCharArray()) {
            assertTrue(ConstantesJuego.CARACTERES_CODIGO_PARTIDA.indexOf(c) >= 0);
        }
    }

    @Test
    void losCodigosNoSeRepitenEntrePartidasActivas() {
        GestorPartidas gestor = crearGestor();
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < ConfiguracionServidor.MAX_PARTIDAS; i++) {
            codigos.add(gestor.creacionPartida().consultaCodigo());
        }
        assertEquals(ConfiguracionServidor.MAX_PARTIDAS, codigos.size());
    }

    @Test
    void noSePuedenCrearMasPartidasQueElMaximo() {
        GestorPartidas gestor = crearGestor();
        for (int i = 0; i < ConfiguracionServidor.MAX_PARTIDAS; i++) {
            assertNotNull(gestor.creacionPartida());
        }
        assertNull(gestor.creacionPartida());
    }

    @Test
    void laBusquedaPorCodigoNoDistingueMayusculas() {
        GestorPartidas gestor = crearGestor();
        SesionPartida sesion = gestor.creacionPartida();
        String minusculas = sesion.consultaCodigo().toLowerCase();
        assertSame(sesion, gestor.partidaPorCodigo(minusculas));
    }

    @Test
    void unaPartidaNuevaEstaEsperandoJugador() {
        GestorPartidas gestor = crearGestor();
        SesionPartida sesion = gestor.creacionPartida();
        assertEquals(EstadoPartida.ESPERANDO_JUGADOR, gestor.estadoPartida(sesion.consultaCodigo()));
    }

    @Test
    void alEliminarUnaPartidaYaNoSeEncuentra() {
        GestorPartidas gestor = crearGestor();
        SesionPartida sesion = gestor.creacionPartida();
        gestor.eliminacionPartida(sesion.consultaCodigo());
        assertNull(gestor.partidaPorCodigo(sesion.consultaCodigo()));
        assertEquals(0, gestor.cantidadPartidasActivas());
    }

    @Test
    void cuentaLosJugadoresConectados() {
        GestorPartidas gestor = crearGestor();
        SesionPartida sesion = gestor.creacionPartida();
        Jugador ana = new Jugador("Ana", null);
        sesion.registroJugador(ana);
        assertEquals(1, gestor.cantidadJugadoresConectados());
        ana.desconexion();
        assertEquals(0, gestor.cantidadJugadoresConectados());
    }
}
