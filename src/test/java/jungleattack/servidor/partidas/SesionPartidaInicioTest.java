package jungleattack.servidor.partidas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.ManadaBots;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.modelo.Taza;
import org.junit.jupiter.api.Test;

class SesionPartidaInicioTest {

    // Sesión con los dos jugadores ya registrados y con especie elegida (jaguar y tucán), lista para iniciar.
    private SesionPartida sesionLista(long semilla) {
        SesionPartida sesion = new SesionPartida("AB12", new Random(semilla));
        Jugador ana = new Jugador("Ana", null);
        Jugador luis = new Jugador("Luis", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(luis);
        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        sesion.eleccionEspecie(luis, Especie.TUCAN);
        return sesion;
    }

    private Zona zonaDe(SesionPartida sesion, Manada manada) {
        return sesion.consultaMapa().zonaPorPosicion(manada.consultaAnimales().get(0).consultaPosicion());
    }

    @Test
    void noInicianSinDosJugadores() {
        SesionPartida sesion = new SesionPartida("AB12", new Random(1));
        sesion.registroJugador(new Jugador("Ana", null));
        assertFalse(sesion.inicio());
        assertEquals(EstadoPartida.ESPERANDO_JUGADOR, sesion.consultaEstado());
    }

    @Test
    void noInicianSiFaltaQueUnJugadorEligaEspecie() {
        SesionPartida sesion = new SesionPartida("AB12", new Random(1));
        Jugador ana = new Jugador("Ana", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(new Jugador("Luis", null));
        sesion.eleccionEspecie(ana, Especie.JAGUAR);

        assertFalse(sesion.inicio());
        assertEquals(EstadoPartida.SELECCION_ESPECIE, sesion.consultaEstado());
        assertNull(sesion.consultaMapa());
    }

    @Test
    void conLaSeleccionCompletaLaPartidaEmpieza() {
        SesionPartida sesion = sesionLista(1);
        assertTrue(sesion.inicio());
        assertEquals(EstadoPartida.EN_CURSO, sesion.consultaEstado());
        assertNotNull(sesion.consultaMapa());
        assertNotNull(sesion.consultaResumen());
    }

    @Test
    void siInicioSeLlamaDosVecesLaSegundaNoHaceNada() {
        SesionPartida sesion = sesionLista(1);
        assertTrue(sesion.inicio());
        assertFalse(sesion.inicio());
        assertEquals(7, sesion.consultaManadas().size());
        assertEquals(10, sesion.consultaTazas().size());
    }

    @Test
    void unaPartidaCanceladaYaNoInicia() {
        SesionPartida sesion = sesionLista(1);
        sesion.cancelacion("OPONENTE_DESCONECTADO");
        assertFalse(sesion.inicio());
    }

    @Test
    void hayUnaManadaPorCadaEspecie() {
        SesionPartida sesion = sesionLista(1);
        sesion.inicio();
        Set<Especie> especies = new HashSet<>();
        for (Manada manada : sesion.consultaManadas()) {
            especies.add(manada.consultaEspecie());
        }
        assertEquals(7, sesion.consultaManadas().size());
        assertEquals(7, especies.size());
    }

    @Test
    void losJugadoresRecibenSuManadaConLasReglasDelJuego() {
        SesionPartida sesion = sesionLista(1);
        sesion.inicio();
        Especie[] elegidas = {Especie.JAGUAR, Especie.TUCAN};
        for (int i = 0; i < 2; i++) {
            Jugador jugador = sesion.consultaJugadores().get(i);
            ManadaJugador manada = jugador.consultaManada();
            assertNotNull(manada);
            assertSame(manada, sesion.manadaPorJugador(jugador));
            assertEquals(elegidas[i], manada.consultaEspecie());
            assertEquals(10, manada.consultaAnimales().size());
            assertEquals(2, manada.consultaDanio());
            for (Animal animal : manada.consultaAnimales()) {
                assertEquals(20, animal.consultaEnergia());
                assertFalse(animal.esBot());
            }
        }
    }

    @Test
    void losBotsSonLasCincoEspeciesQueNadieEligio() {
        SesionPartida sesion = sesionLista(1);
        sesion.inicio();
        Set<Especie> especiesBots = new HashSet<>();
        for (Manada manada : sesion.consultaManadas()) {
            if (manada instanceof ManadaBots) {
                especiesBots.add(manada.consultaEspecie());
                assertEquals(4, manada.consultaAnimales().size());
                assertEquals(1, manada.consultaDanio());
                for (Animal animal : manada.consultaAnimales()) {
                    assertEquals(10, animal.consultaEnergia());
                    assertTrue(animal.esBot());
                }
            }
        }
        Set<Especie> esperadas = new HashSet<>(Set.of(Especie.values()));
        esperadas.remove(Especie.JAGUAR);
        esperadas.remove(Especie.TUCAN);
        assertEquals(esperadas, especiesBots);
    }

    @Test
    void lasSieteManadasEmpiezanEnZonasDistintas() {
        for (long semilla = 0; semilla < 30; semilla++) {
            SesionPartida sesion = sesionLista(semilla);
            sesion.inicio();
            Set<Integer> zonas = new HashSet<>();
            for (Manada manada : sesion.consultaManadas()) {
                zonas.add(zonaDe(sesion, manada).consultaIdentificador());
            }
            assertEquals(7, zonas.size(), "semilla " + semilla);
        }
    }

    @Test
    void todosLosAnimalesDeUnaManadaEmpiezanEnLaMismaZonaYRegistradosEnElla() {
        SesionPartida sesion = sesionLista(3);
        sesion.inicio();
        for (Manada manada : sesion.consultaManadas()) {
            Zona zona = zonaDe(sesion, manada);
            for (Animal animal : manada.consultaAnimales()) {
                assertTrue(zona.contienePunto(animal.consultaPosicion()));
                assertTrue(sesion.animalesEnZona(zona).contains(animal));
            }
        }
    }

    @Test
    void cadaJugadorTieneSuBanderaEnElCentroDeSuZonaDeInicio() {
        SesionPartida sesion = sesionLista(5);
        sesion.inicio();
        assertEquals(2, sesion.consultaBanderas().size());
        Set<Integer> zonasBanderas = new HashSet<>();
        for (Bandera bandera : sesion.consultaBanderas()) {
            Jugador dueno = bandera.consultaPropietario();
            assertSame(zonaDe(sesion, dueno.consultaManada()), bandera.consultaZona());
            assertEquals(bandera.consultaZona().consultaPosicion().consultaPosicionX(), bandera.consultaPosicion().consultaPosicionX());
            assertEquals(bandera.consultaZona().consultaPosicion().consultaPosicionY(), bandera.consultaPosicion().consultaPosicionY());
            zonasBanderas.add(bandera.consultaZona().consultaIdentificador());
        }
        assertEquals(2, zonasBanderas.size());
    }

    @Test
    void lasDiezTazasQuedanRegistradasEnLasZonas() {
        SesionPartida sesion = sesionLista(5);
        sesion.inicio();
        assertEquals(ConstantesJuego.CANTIDAD_TAZAS, sesion.consultaTazas().size());
        int enZonas = 0;
        for (Zona zona : sesion.consultaMapa().consultaZonas()) {
            for (Taza taza : zona.tazasEnZona()) {
                assertSame(zona, sesion.consultaMapa().zonaPorPosicion(taza.consultaPosicion()));
                enZonas++;
            }
        }
        assertEquals(10, enZonas);
    }

    @Test
    void conLaMismaSemillaLaPartidaEmpiezaIgualYConOtrasVaria() {
        SesionPartida a = sesionLista(11);
        SesionPartida b = sesionLista(11);
        a.inicio();
        b.inicio();
        assertEquals(zonaDe(a, a.consultaJugadores().get(0).consultaManada()).consultaIdentificador(),
                zonaDe(b, b.consultaJugadores().get(0).consultaManada()).consultaIdentificador());

        Set<Integer> zonasDeAna = new HashSet<>();
        for (long semilla = 0; semilla < 30; semilla++) {
            SesionPartida otra = sesionLista(semilla);
            otra.inicio();
            zonasDeAna.add(zonaDe(otra, otra.consultaJugadores().get(0).consultaManada()).consultaIdentificador());
        }
        assertTrue(zonasDeAna.size() > 1);
    }

    @Test
    void sePuedeBuscarUnAnimalDeLaPartidaPorSuIdentificador() {
        SesionPartida sesion = sesionLista(2);
        sesion.inicio();
        assertEquals("jaguar-1", sesion.animalPorIdentificador("jaguar-1").consultaIdentificador());
        assertEquals("tucan-4", sesion.animalPorIdentificador("tucan-4").consultaIdentificador());
        assertNull(sesion.animalPorIdentificador("dragon-1"));
    }
}
