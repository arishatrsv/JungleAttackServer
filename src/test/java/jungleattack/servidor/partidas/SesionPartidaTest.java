package jungleattack.servidor.partidas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import org.junit.jupiter.api.Test;

class SesionPartidaTest {

    // Crea jugadores sin conexión para probar la lógica de la sesión.
    private Jugador jugador(String apodo) {
        return new Jugador(apodo, null);
    }

    @Test
    void unaPartidaNuevaEsperaJugadores() {
        SesionPartida sesion = new SesionPartida("AB12");
        // La sesión conserva su código y espera a que se unan los jugadores.
        assertEquals(EstadoPartida.ESPERANDO_JUGADOR, sesion.consultaEstado());
        assertEquals("AB12", sesion.consultaCodigo());
    }

    @Test
    void elSegundoJugadorIniciaLaSeleccionDeEspecie() {
        SesionPartida sesion = new SesionPartida("AB12");
        assertTrue(sesion.registroJugador(jugador("Ana")));
        assertEquals(EstadoPartida.ESPERANDO_JUGADOR, sesion.consultaEstado());
        assertTrue(sesion.registroJugador(jugador("Jugador2")));
        // Con dos jugadores registrados, comienza la selección de especies.
        assertEquals(EstadoPartida.SELECCION_ESPECIE, sesion.consultaEstado());
    }

    @Test
    void unTercerJugadorNoPuedeEntrar() {
        SesionPartida sesion = new SesionPartida("AB12");
        sesion.registroJugador(jugador("Ana"));
        sesion.registroJugador(jugador("Jugador2"));
        // La partida admite como máximo dos jugadores.
        assertFalse(sesion.registroJugador(jugador("Luis")));
        assertEquals(2, sesion.consultaJugadores().size());
    }

    @Test
    void noSePuedeEscogerEspecieAntesDeLaSeleccion() {
        SesionPartida sesion = new SesionPartida("AB12");
        Jugador ana = jugador("Ana");
        sesion.registroJugador(ana);
        // La selección todavía no está habilitada para una partida incompleta.
        assertFalse(sesion.eleccionEspecie(ana, Especie.JAGUAR));
    }

    @Test
    void dosJugadoresNoPuedenTenerLaMismaEspecie() {
        SesionPartida sesion = new SesionPartida("AB12");
        Jugador ana = jugador("Ana");
        Jugador segundoJugador = jugador("Jugador2");
        sesion.registroJugador(ana);
        sesion.registroJugador(segundoJugador);

        assertTrue(sesion.eleccionEspecie(ana, Especie.JAGUAR));
        // Una especie ya elegida no puede asignarse al otro jugador.
        assertFalse(sesion.eleccionEspecie(segundoJugador, Especie.JAGUAR));
        assertTrue(sesion.eleccionEspecie(segundoJugador, Especie.TUCAN));
    }

    @Test
    void lasEspeciesEscogidasDejanDeEstarDisponibles() {
        SesionPartida sesion = new SesionPartida("AB12");
        Jugador ana = jugador("Ana");
        sesion.registroJugador(ana);
        sesion.registroJugador(jugador("Jugador2"));
        assertEquals(7, sesion.especiesDisponibles().size());

        sesion.eleccionEspecie(ana, Especie.JAGUAR);

        // La especie elegida se quita de la lista compartida de disponibles.
        assertEquals(6, sesion.especiesDisponibles().size());
        assertFalse(sesion.especiesDisponibles().contains(Especie.JAGUAR));
    }

    @Test
    void laSeleccionEstaCompletaCuandoLosDosEligieron() {
        SesionPartida sesion = new SesionPartida("AB12");
        Jugador ana = jugador("Ana");
        Jugador segundoJugador = jugador("Jugador2");
        sesion.registroJugador(ana);
        sesion.registroJugador(segundoJugador);

        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        assertFalse(sesion.seleccionCompleta());
        sesion.eleccionEspecie(segundoJugador, Especie.TUCAN);
        // La selección queda completa cuando ambos jugadores eligen una especie.
        assertTrue(sesion.seleccionCompleta());
    }

    @Test
    void cancelarLaPartidaLaDejaFinalizada() {
        SesionPartida sesion = new SesionPartida("AB12");
        sesion.registroJugador(jugador("Ana"));
        sesion.cancelacion("OPONENTE_DESCONECTADO");
        assertEquals(EstadoPartida.FINALIZADA, sesion.consultaEstado());
        // Una partida finalizada ya no admite nuevos jugadores.
        assertFalse(sesion.registroJugador(jugador("Jugador2")));
    }

    @Test
    void cuentaSoloLosJugadoresConectados() {
        SesionPartida sesion = new SesionPartida("AB12");
        Jugador ana = jugador("Ana");
        sesion.registroJugador(ana);
        sesion.registroJugador(jugador("Jugador2"));
        ana.desconexion();
        // El conteo excluye al jugador que se desconectó.
        assertEquals(1, sesion.cantidadJugadoresConectados());
    }
}
