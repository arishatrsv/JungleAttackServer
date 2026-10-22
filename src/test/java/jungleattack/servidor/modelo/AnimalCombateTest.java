package jungleattack.servidor.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import jungleattack.servidor.mapa.Punto;
import org.junit.jupiter.api.Test;

class AnimalCombateTest {

    private Animal animal(String id, int energia) {
        return new AnimalJugador(id, Especie.JAGUAR, energia, new Punto(100, 100));
    }

    @Test
    void elDanioRestaEnergiaYNoBajaDeCero() {
        Animal animal = animal("jaguar-1", 5);
        assertFalse(animal.recepcionDanio(2));
        assertEquals(3, animal.consultaEnergia());
        assertTrue(animal.consultaVida());
        assertTrue(animal.recepcionDanio(10)); // más daño que energía
        assertEquals(0, animal.consultaEnergia());
        assertFalse(animal.consultaVida());
    }

    @Test
    void elGolpeQueMataDevuelveTrueUnaSolaVez() {
        Animal animal = animal("jaguar-1", 2);
        assertTrue(animal.recepcionDanio(2));
        assertFalse(animal.recepcionDanio(2)); // ya estaba muerto
        assertEquals(0, animal.consultaEnergia());
    }

    @Test
    void alMorirSeDetieneYDejaDeAtacar() {
        Animal animal = animal("jaguar-1", 2);
        animal.asignacionRuta(List.of(new Punto(100, 100), new Punto(200, 100)), 0);
        animal.ordenAtaque(animal("jaguar-2", 5));
        assertTrue(animal.consultaEnMovimiento());
        animal.recepcionDanio(5);
        assertFalse(animal.consultaEnMovimiento());
        assertNull(animal.consultaObjetivoAtaque());
    }

    @Test
    void elPrimerAtaqueEsInmediatoYLuegoHayQueEsperarElEnfriamiento() {
        Animal animal = animal("jaguar-1", 20);
        assertTrue(animal.intentoAtaque(0, 1000));
        assertFalse(animal.intentoAtaque(500, 1000));
        assertFalse(animal.intentoAtaque(999, 1000));
        assertTrue(animal.intentoAtaque(1000, 1000));
        assertFalse(animal.intentoAtaque(1500, 1000));
    }

    @Test
    void sePuedeOrdenarYDetenerUnAtaque() {
        Animal atacante = animal("jaguar-1", 20);
        Animal objetivo = animal("tucan-1", 10);
        assertNull(atacante.consultaObjetivoAtaque());
        atacante.ordenAtaque(objetivo);
        assertSame(objetivo, atacante.consultaObjetivoAtaque());
        atacante.detencionAtaque();
        assertNull(atacante.consultaObjetivoAtaque());
    }

    @Test
    void conVariosHilosHiriendoALaVezElAnimalMuereUnaSolaVez() throws Exception {
        for (int vuelta = 0; vuelta < 30; vuelta++) {
            Animal victima = animal("jaguar-1", 6);
            CountDownLatch salida = new CountDownLatch(1);
            AtomicInteger muertes = new AtomicInteger();
            List<Thread> hilos = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Thread hilo = new Thread(() -> {
                    try {
                        salida.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (victima.recepcionDanio(2)) {
                        muertes.incrementAndGet();
                    }
                });
                hilo.start();
                hilos.add(hilo);
            }
            salida.countDown();
            for (Thread hilo : hilos) {
                hilo.join();
            }
            assertEquals(1, muertes.get());
            assertEquals(0, victima.consultaEnergia());
        }
    }
}
