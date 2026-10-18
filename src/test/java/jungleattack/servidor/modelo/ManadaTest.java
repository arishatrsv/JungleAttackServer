package jungleattack.servidor.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import org.junit.jupiter.api.Test;

class ManadaTest {

    private final Punto centro = new Punto(375, 240);

    @Test
    void laManadaDeUnJugadorTieneDiezAnimalesConVeinteDeEnergiaYDanioDos() {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        manada.creacionAnimales(ConstantesJuego.CANTIDAD_ANIMALES_JUGADOR, centro);

        assertEquals(10, manada.consultaAnimales().size());
        assertEquals(2, manada.consultaDanio());
        for (Animal animal : manada.consultaAnimales()) {
            assertEquals(20, animal.consultaEnergia());
            assertTrue(animal.consultaVida());
            assertFalse(animal.esBot());
            assertTrue(animal instanceof AnimalJugador);
            assertSame(manada, animal.consultaManada());
            assertEquals(Especie.JAGUAR, animal.consultaEspecie());
        }
    }

    @Test
    void laManadaDeBotsTieneCuatroAnimalesConDiezDeEnergiaYDanioUno() {
        ManadaBots manada = new ManadaBots(Especie.TUCAN);
        manada.creacionAnimales(ConstantesJuego.CANTIDAD_ANIMALES_BOTS, centro);

        assertEquals(4, manada.consultaAnimales().size());
        assertEquals(1, manada.consultaDanio());
        assertEquals(10, manada.consultaEnergiaInicial());
        for (Animal animal : manada.consultaAnimales()) {
            assertEquals(10, animal.consultaEnergia());
            assertTrue(animal.esBot());
            assertTrue(animal instanceof AnimalBot);
        }
    }

    @Test
    void losIdentificadoresSonUnicosYUsanLaEspecie() {
        ManadaJugador manada = new ManadaJugador(Especie.RANA_OJOS_ROJOS);
        manada.creacionAnimales(10, centro);
        Set<String> identificadores = new HashSet<>();
        for (Animal animal : manada.consultaAnimales()) {
            identificadores.add(animal.consultaIdentificador());
        }
        assertEquals(10, identificadores.size());
        assertTrue(identificadores.contains("rana_ojos_rojos-1"));
        assertTrue(identificadores.contains("rana_ojos_rojos-10"));
    }

    @Test
    void losAnimalesNoQuedanTodosEnElMismoPunto() {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        manada.creacionAnimales(10, centro);
        Set<String> posiciones = new HashSet<>();
        for (Animal animal : manada.consultaAnimales()) {
            posiciones.add(animal.consultaPosicion().consultaPosicionX() + "," + animal.consultaPosicion().consultaPosicionY());
        }
        assertEquals(10, posiciones.size());
    }

    @Test
    void laFormacionQuedaDentroDeCadaUnaDeLasDoceZonas() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        for (Zona zona : mapa.consultaZonas()) {
            ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
            manada.creacionAnimales(ConstantesJuego.CANTIDAD_ANIMALES_JUGADOR, zona.consultaPosicion());
            for (Animal animal : manada.consultaAnimales()) {
                assertTrue(zona.contienePunto(animal.consultaPosicion()),
                        "animal fuera de la zona " + zona.consultaIdentificador());
            }
        }
    }

    @Test
    void unAnimalSoloEstaARadioDeFormacionDelCentro() {
        Punto posicion = Manada.posicionEnFormacion(centro, 0, 1);
        assertEquals(centro.consultaPosicionX() + ConstantesJuego.RADIO_FORMACION, posicion.consultaPosicionX());
        assertEquals(centro.consultaPosicionY(), posicion.consultaPosicionY());
    }

    @Test
    void losAnimalesMuertosNoCuentanComoVivos() {
        ManadaBots manada = new ManadaBots(Especie.LAPA_ROJA);
        manada.creacionAnimales(4, centro);
        manada.consultaAnimales().get(0).actualizacionEnergia(0);
        assertEquals(3, manada.cantidadAnimalesVivos());
        assertEquals(3, manada.animalesVivos().size());
    }

    @Test
    void sePuedeBuscarUnAnimalPorSuIdentificador() {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        manada.creacionAnimales(10, centro);
        assertSame(manada.consultaAnimales().get(2), manada.animalPorIdentificador("jaguar-3"));
        assertNull(manada.animalPorIdentificador("tucan-1"));
    }

    @Test
    void losAnimalesSeleccionadosIgnoranLosDesconocidosLosRepetidosYLosMuertos() {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        manada.creacionAnimales(10, centro);
        manada.animalPorIdentificador("jaguar-2").actualizacionEnergia(0);

        List<Animal> seleccionados = manada.animalesSeleccionados(
                List.of("jaguar-1", "jaguar-1", "jaguar-2", "tucan-9", "jaguar-3"));

        assertEquals(2, seleccionados.size());
        assertEquals("jaguar-1", seleccionados.get(0).consultaIdentificador());
        assertEquals("jaguar-3", seleccionados.get(1).consultaIdentificador());
    }
}
