package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.concurrent.atomic.AtomicLong;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalJugador;
import jungleattack.servidor.modelo.ManadaJugador;
import org.junit.jupiter.api.Test;

class HiloZonaTest {

    private final Mapa mapa = crearMapa();
    private final ReglasMovimiento reglas = new ReglasMovimiento(mapa);
    private final AtomicLong reloj = new AtomicLong(0);

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    // Un animal registrado en la zona 12 (Corcovado), en su centro.
    private Animal animalEnLaZona12() {
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        Animal animal = new AnimalJugador("jaguar-1", Especie.JAGUAR, 20, new Punto(695, 420));
        manada.incorporacionAnimal(animal);
        mapa.zonaPorIdentificador(12).registroAnimal(animal);
        return animal;
    }

    @Test
    void unCicloMueveALosAnimalesDeSuZona() {
        Animal animal = animalEnLaZona12();
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        new HiloZona(mapa.zonaPorIdentificador(12), mapa, reglas, reloj::get).actualizacionZona(1000);
        assertEquals(635, animal.consultaPosicion().consultaPosicionX());
    }

    @Test
    void unCicloNoTocaAnimalesDeOtraZona() {
        Animal animal = animalEnLaZona12();
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        new HiloZona(mapa.zonaPorIdentificador(11), mapa, reglas, reloj::get).actualizacionZona(1000);
        assertEquals(695, animal.consultaPosicion().consultaPosicionX());
    }

    @Test
    void alCruzarElBordeElAnimalPasaDeUnaZonaALaOtra() {
        Animal animal = animalEnLaZona12();
        Zona doce = mapa.zonaPorIdentificador(12);
        Zona once = mapa.zonaPorIdentificador(11);
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        HiloZona hiloDoce = new HiloZona(doce, mapa, reglas, reloj::get);
        HiloZona hiloOnce = new HiloZona(once, mapa, reglas, reloj::get);

        long tiempo = 0;
        while (animal.consultaEnMovimiento() && tiempo < 10000) {
            tiempo += 50;
            hiloDoce.actualizacionZona(tiempo);
            hiloOnce.actualizacionZona(tiempo);
        }
        assertEquals(535, animal.consultaPosicion().consultaPosicionX());
        assertTrue(once.animalesEnZona().contains(animal));
        assertFalse(doce.animalesEnZona().contains(animal));
    }

    @Test
    void elHiloCorreSolitoYSeDetieneAlCerrar() throws Exception {
        Animal animal = animalEnLaZona12();
        long inicio = System.currentTimeMillis();
        reglas.inicioMovimiento(animal, new Punto(535, 420), inicio, 0, 1);
        HiloZona hiloZona = new HiloZona(mapa.zonaPorIdentificador(12), mapa, reglas, System::currentTimeMillis);
        Thread hilo = new Thread(hiloZona);
        hilo.setDaemon(true);
        hilo.start();
        Thread.sleep(400);
        hiloZona.cierre();
        hilo.join(2000);
        assertFalse(hilo.isAlive());
        assertTrue(animal.consultaPosicion().consultaPosicionX() < 695, "el animal debió avanzar con el hilo");
    }
}
