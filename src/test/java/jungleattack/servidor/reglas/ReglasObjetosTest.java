package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalBot;
import jungleattack.servidor.modelo.ManadaBots;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.modelo.Taza;
import org.junit.jupiter.api.Test;

class ReglasTazasTest {

    private final Mapa mapa = crearMapa();
    private final ReglasObjetos reglas = new ReglasObjetos();

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    // Una manada de jugador con n animales, todos en (x, y).
    private ManadaJugador manada(int cantidad, int x, int y) {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        for (int i = 1; i <= cantidad; i++) {
            manada.incorporacionAnimal(new jungleattack.servidor.modelo.AnimalJugador("jaguar-" + i, Especie.JAGUAR, 20, new Punto(x, y)));
        }
        return manada;
    }

    // Una taza sobre el camino que une las zonas 11 y 12, registrada en su zona.
    private Taza taza(String id, int x, int y) {
        Taza taza = new Taza(id, new Punto(x, y));
        mapa.zonaPorPosicion(taza.consultaPosicion()).registroTaza(taza);
        return taza;
    }

    // ----- La taza -----

    @Test
    void unaTazaSoloSePuedeConsumirUnaVez() {
        Taza taza = new Taza("taza-1", new Punto(10, 10));
        assertFalse(taza.consultaConsumida());
        assertTrue(taza.consumo());
        assertTrue(taza.consultaConsumida());
        assertFalse(taza.consumo());
    }

    // ----- Consumir -----

    @Test
    void alConsumirUnaTazaElDanioDeTodaLaManadaSubeEnUno() {
        ManadaJugador manada = manada(3, 600, 420);
        assertEquals(2, manada.consultaDanio());
        assertTrue(reglas.consumoTaza(manada.consultaAnimales().get(0), taza("taza-1", 600, 420)));
        assertEquals(3, manada.consultaDanio()); // el daño es de la manada: lo tienen los 3 animales
        assertEquals(1, manada.consultaTazasConsumidas());
    }

    @Test
    void elDanioPorTazasEsAcumulativo() {
        ManadaJugador manada = manada(2, 600, 420);
        reglas.consumoTaza(manada.consultaAnimales().get(0), taza("taza-1", 600, 420));
        reglas.consumoTaza(manada.consultaAnimales().get(1), taza("taza-2", 620, 420));
        assertEquals(4, manada.consultaDanio());
        assertEquals(2, manada.consultaTazasConsumidas());
    }

    @Test
    void unaTazaYaConsumidaNoDaNadaDeNuevo() {
        ManadaJugador manada = manada(2, 600, 420);
        Taza taza = taza("taza-1", 600, 420);
        assertTrue(reglas.consumoTaza(manada.consultaAnimales().get(0), taza));
        assertFalse(reglas.consumoTaza(manada.consultaAnimales().get(1), taza));
        assertEquals(3, manada.consultaDanio());
        assertEquals(1, manada.consultaTazasConsumidas());
    }

    @Test
    void unAnimalMuertoNoConsumeTazas() {
        ManadaJugador manada = manada(1, 600, 420);
        Animal muerto = manada.consultaAnimales().get(0);
        muerto.actualizacionEnergia(0);
        Taza taza = taza("taza-1", 600, 420);
        assertFalse(reglas.consumoTaza(muerto, taza));
        assertFalse(taza.consultaConsumida());
        assertEquals(2, manada.consultaDanio());
    }

    @Test
    void losBotsNoConsumenTazas() {
        ManadaBots bots = new ManadaBots(Especie.TUCAN);
        Animal bot = new AnimalBot("tucan-1", Especie.TUCAN, 10, new Punto(600, 420));
        bots.incorporacionAnimal(bot);
        Taza taza = taza("taza-1", 600, 420);
        assertFalse(reglas.consumoTaza(bot, taza));
        assertFalse(taza.consultaConsumida());
        assertEquals(1, bots.consultaDanio());
        assertEquals(0, reglas.recogidaTazas(bot, List.of(taza), mapa));
    }

    @Test
    void dosAnimalesQueTocanLaMismaTazaAlMismoTiempoSoloLaConsumeUno() throws Exception {
        for (int vuelta = 0; vuelta < 20; vuelta++) {
            ManadaJugador manada = manada(8, 600, 420);
            Taza taza = taza("taza-" + vuelta, 600, 420);
            CountDownLatch salida = new CountDownLatch(1);
            AtomicInteger consumos = new AtomicInteger();
            List<Thread> hilos = new ArrayList<>();
            for (Animal animal : manada.consultaAnimales()) {
                Thread hilo = new Thread(() -> {
                    try {
                        salida.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (reglas.consumoTaza(animal, taza)) {
                        consumos.incrementAndGet();
                    }
                });
                hilo.start();
                hilos.add(hilo);
            }
            salida.countDown();
            for (Thread hilo : hilos) {
                hilo.join();
            }
            assertEquals(1, consumos.get());
            assertEquals(3, manada.consultaDanio());
            assertEquals(1, manada.consultaTazasConsumidas());
        }
    }

    // ----- Recoger tazas al moverse -----

    @Test
    void unAnimalCercaDeUnaTazaLaRecoge() {
        ManadaJugador manada = manada(1, 600, 420);
        Taza taza = taza("taza-1", 612, 420); // a 12 unidades: dentro del radio
        assertEquals(1, reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(taza), mapa));
        assertTrue(taza.consultaConsumida());
        assertEquals(3, manada.consultaDanio());
    }

    @Test
    void unAnimalLejosDeUnaTazaNoLaRecoge() {
        ManadaJugador manada = manada(1, 600, 420);
        Taza taza = taza("taza-1", 640, 420); // a 40 unidades
        assertEquals(0, reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(taza), mapa));
        assertFalse(taza.consultaConsumida());
        assertEquals(2, manada.consultaDanio());
    }

    @Test
    void elLimiteDelRadioCuentaComoTocar() {
        ManadaJugador manada = manada(1, 600, 420);
        Taza justo = taza("taza-1", 615, 420); // exactamente a 15
        Taza fuera = taza("taza-2", 616, 441); // a más de 15
        assertEquals(1, reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(justo, fuera), mapa));
        assertTrue(justo.consultaConsumida());
        assertFalse(fuera.consultaConsumida());
    }

    @Test
    void unaTazaConsumidaDesapareceDeSuZona() {
        ManadaJugador manada = manada(1, 600, 420);
        Taza taza = taza("taza-1", 600, 420);
        Zona zona = mapa.zonaPorPosicion(taza.consultaPosicion());
        assertTrue(zona.tazasEnZona().contains(taza));
        reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(taza), mapa);
        assertFalse(zona.tazasEnZona().contains(taza));
    }

    @Test
    void unAnimalPuedeRecogerVariasTazasEnUnaMismaRevision() {
        ManadaJugador manada = manada(1, 600, 420);
        List<Taza> tazas = List.of(taza("taza-1", 600, 425), taza("taza-2", 605, 420), taza("taza-3", 700, 420));
        assertEquals(2, reglas.recogidaTazas(manada.consultaAnimales().get(0), tazas, mapa));
        assertEquals(4, manada.consultaDanio());
        assertFalse(tazas.get(2).consultaConsumida());
    }

    @Test
    void unaTazaYaConsumidaNoSeRecogeDeNuevo() {
        ManadaJugador manada = manada(2, 600, 420);
        Taza taza = taza("taza-1", 600, 420);
        assertEquals(1, reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(taza), mapa));
        assertEquals(0, reglas.recogidaTazas(manada.consultaAnimales().get(1), List.of(taza), mapa));
        assertEquals(3, manada.consultaDanio());
    }

    @Test
    void laTazaDeUnaZonaVecinaTambienSeRecogeSiEstaCerca() {
        // El borde entre las zonas 11 y 12 pasa por x = 615: la taza queda en una zona y el animal en la otra.
        ManadaJugador manada = manada(1, 625, 420);
        Taza taza = taza("taza-1", 610, 420);
        assertEquals(1, reglas.recogidaTazas(manada.consultaAnimales().get(0), List.of(taza), mapa));
    }
}
