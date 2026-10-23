package jungleattack.servidor.partidas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SesionPartidaFinalTest {

    private final AtomicLong reloj = new AtomicLong(0);

    // Partida en curso, con el reloj controlado por la prueba. motorActivo decide si el motor arranca sus hilos.
    private SesionPartida enCurso(boolean motorActivo) {
        SesionPartida sesion = new SesionPartida("AB12", new Random(4), motorActivo, reloj::get);
        Jugador ana = new Jugador("Ana", null);
        Jugador luis = new Jugador("Luis", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(luis);
        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        sesion.eleccionEspecie(luis, Especie.TUCAN);
        sesion.inicio();
        return sesion;
    }

    @Test
    void alFinalizarLaPartidaQuedaTerminadaConSuResumen() {
        SesionPartida sesion = enCurso(false);
        Jugador ana = sesion.consultaJugadores().get(0);
        reloj.set(42000);

        assertTrue(sesion.finalizacion(ana, ConstantesProtocolo.RAZON_BANDERA));

        assertEquals(EstadoPartida.FINALIZADA, sesion.consultaEstado());
        assertNotNull(sesion.consultaResumen());
        assertSame(ana, sesion.consultaResumen().consultaGanador());
        assertEquals(ConstantesProtocolo.RAZON_BANDERA, sesion.consultaResumen().consultaRazon());
        assertEquals(42.0, sesion.consultaResumen().consultaDuracion(), 0.0001);
    }

    @Test
    void ganaSoloElPrimeroSiDosJugadoresTocanLaBanderaAlMismoTiempo() throws Exception {
        for (int vuelta = 0; vuelta < 30; vuelta++) {
            SesionPartida sesion = enCurso(false);
            Jugador ana = sesion.consultaJugadores().get(0);
            Jugador luis = sesion.consultaJugadores().get(1);
            CountDownLatch salida = new CountDownLatch(1);
            AtomicInteger ganadas = new AtomicInteger();
            List<Thread> hilos = new ArrayList<>();
            for (Jugador candidato : List.of(ana, luis)) {
                Thread hilo = new Thread(() -> {
                    try {
                        salida.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (sesion.finalizacion(candidato, ConstantesProtocolo.RAZON_BANDERA)) {
                        ganadas.incrementAndGet();
                    }
                });
                hilo.start();
                hilos.add(hilo);
            }
            salida.countDown();
            for (Thread hilo : hilos) {
                hilo.join();
            }
            assertEquals(1, ganadas.get());
            assertNotNull(sesion.consultaResumen().consultaGanador());
        }
    }

    @Test
    void unaPartidaYaTerminadaNoSePuedeTerminarOtraVez() {
        SesionPartida sesion = enCurso(false);
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        assertTrue(sesion.finalizacion(ana, ConstantesProtocolo.RAZON_BANDERA));
        assertFalse(sesion.finalizacion(luis, ConstantesProtocolo.RAZON_ABANDONO));
        assertSame(ana, sesion.consultaResumen().consultaGanador()); // el resultado no cambió
    }

    @Test
    void sinPartidaEnCursoNoSePuedeFinalizar() {
        SesionPartida sesion = new SesionPartida("AB12", new Random(1), false, reloj::get);
        Jugador ana = new Jugador("Ana", null);
        sesion.registroJugador(ana);
        assertFalse(sesion.finalizacion(ana, ConstantesProtocolo.RAZON_BANDERA));
        assertNull(sesion.consultaResumen());
        assertEquals(EstadoPartida.ESPERANDO_JUGADOR, sesion.consultaEstado());
    }

    @Test
    void alFinalizarSeDetieneElMotor() {
        SesionPartida sesion = enCurso(true);
        assertTrue(sesion.consultaMotor().consultaActivo());
        sesion.finalizacion(sesion.consultaJugadores().get(0), ConstantesProtocolo.RAZON_BANDERA);
        assertFalse(sesion.consultaMotor().consultaActivo());
    }

    @Test
    void alFinalizarSeAvisaUnaSolaVez() {
        SesionPartida sesion = enCurso(false);
        AtomicInteger avisos = new AtomicInteger();
        sesion.registroAlFinalizar(avisos::incrementAndGet);
        sesion.finalizacion(sesion.consultaJugadores().get(0), ConstantesProtocolo.RAZON_BANDERA);
        sesion.finalizacion(sesion.consultaJugadores().get(1), ConstantesProtocolo.RAZON_BANDERA);
        assertEquals(1, avisos.get());
    }

    @Test
    void elGestorSacaLaPartidaDeLasActivasCuandoTermina() {
        GestorPartidas gestor = new GestorPartidas(new Random(3));
        SesionPartida sesion = gestor.creacionPartida();
        Jugador ana = new Jugador("Ana", null);
        Jugador luis = new Jugador("Luis", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(luis);
        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        sesion.eleccionEspecie(luis, Especie.TUCAN);
        sesion.inicio();
        assertEquals(1, gestor.cantidadPartidasActivas());

        sesion.finalizacion(ana, ConstantesProtocolo.RAZON_BANDERA);

        assertEquals(0, gestor.cantidadPartidasActivas());
        assertNull(gestor.partidaPorCodigo(sesion.consultaCodigo()));
    }
}
