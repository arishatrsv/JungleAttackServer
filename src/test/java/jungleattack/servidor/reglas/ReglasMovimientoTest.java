package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.partidas.Jugador;
import org.junit.jupiter.api.Test;

class ReglasMovimientoTest {

    private final Mapa mapa = crearMapa();
    private final ReglasMovimiento reglas = new ReglasMovimiento(mapa);

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    // Un jugador con una manada de n animales ubicados todos en (x, y).
    private Jugador jugadorConAnimales(int cantidad, int x, int y) {
        Jugador jugador = new Jugador("Ana", null);
        ManadaJugador manada = new ManadaJugador(Especie.JAGUAR);
        for (int i = 1; i <= cantidad; i++) {
            manada.incorporacionAnimal(new jungleattack.servidor.modelo.AnimalJugador("jaguar-" + i, Especie.JAGUAR, 20, new Punto(x, y)));
        }
        jugador.asignacionManada(manada);
        return jugador;
    }

    private Animal primerAnimal(Jugador jugador) {
        return jugador.consultaManada().consultaAnimales().get(0);
    }

    private void assertPosicion(int x, int y, Animal animal) {
        assertEquals(x, animal.consultaPosicion().consultaPosicionX(), "x");
        assertEquals(y, animal.consultaPosicion().consultaPosicionY(), "y");
    }

    @Test
    void elAnimalAvanzaLaVelocidadPorElTiempoTranscurrido() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1); // 160 unidades hacia la izquierda
        assertTrue(reglas.actualizacionPosicion(animal, 1000)); // 60 unidades por segundo
        assertPosicion(635, 420, animal);
        reglas.actualizacionPosicion(animal, 1500);
        assertPosicion(605, 420, animal);
    }

    @Test
    void laVelocidadNoDependeDeCuantasVecesSeActualiza() {
        Animal pocas = primerAnimal(jugadorConAnimales(1, 695, 420));
        Animal muchas = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(pocas, new Punto(535, 420), 0, 0, 1);
        reglas.inicioMovimiento(muchas, new Punto(535, 420), 0, 0, 1);
        reglas.actualizacionPosicion(pocas, 2000);
        for (int t = 50; t <= 2000; t += 50) {
            reglas.actualizacionPosicion(muchas, t);
        }
        assertEquals(pocas.consultaPosicion().consultaPosicionX(), muchas.consultaPosicion().consultaPosicionX());
        assertPosicion(575, 420, muchas);
    }

    @Test
    void unPasoMuyPequenoNoDejaAlAnimalQuieto() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        for (int t = 5; t <= 1000; t += 5) { // 0,3 unidades por paso: menos que un píxel
            reglas.actualizacionPosicion(animal, t);
        }
        assertPosicion(635, 420, animal);
    }

    @Test
    void alLlegarSeDetieneYYaNoCambia() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        assertTrue(animal.consultaEnMovimiento());
        reglas.actualizacionPosicion(animal, 10000); // mucho más tiempo del necesario
        assertPosicion(535, 420, animal);
        assertFalse(animal.consultaEnMovimiento());
        assertFalse(reglas.actualizacionPosicion(animal, 20000));
        assertPosicion(535, 420, animal);
    }

    @Test
    void sinTiempoTranscurridoNoSeMueve() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 500, 0, 1);
        assertFalse(reglas.actualizacionPosicion(animal, 500));
        assertPosicion(695, 420, animal);
    }

    @Test
    void siElAnimalYaEstaEnElDestinoNoSeMueve() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(695, 420), 0, 0, 1);
        assertFalse(animal.consultaEnMovimiento());
    }

    @Test
    void doblaEnLasZonasDeLaRutaSinPerderVelocidad() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(695, 140), 0, 0, 1); // 12 -> 9 (180) -> 6 (100)
        reglas.actualizacionPosicion(animal, 3000);
        assertPosicion(695, 240, animal);
        reglas.actualizacionPosicion(animal, 4000);
        assertPosicion(695, 180, animal);
    }

    @Test
    void unPasoGrandePuedeCruzarVariosPuntosDeLaRuta() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 375, 330));
        reglas.inicioMovimiento(animal, new Punto(695, 240), 0, 0, 1); // 7 -> 4 -> 8 -> 9
        reglas.actualizacionPosicion(animal, 7000); // 420 unidades de una sola vez: el camino mide 410
        assertPosicion(695, 240, animal);
        assertFalse(animal.consultaEnMovimiento());
    }

    @Test
    void unAnimalFueraDeLosCaminosPrimeroVaAlCamino() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 725, 420)); // en formación, a 30 del centro de la zona 12
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        reglas.actualizacionPosicion(animal, 500); // 30 unidades: llega justo al camino
        assertPosicion(695, 420, animal);
    }

    @Test
    void unNuevoDestinoReemplazaLaRutaAnterior() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        reglas.actualizacionPosicion(animal, 1000);
        assertPosicion(635, 420, animal);
        reglas.inicioMovimiento(animal, new Punto(695, 240), 1000, 0, 1); // ahora hacia el otro lado
        reglas.actualizacionPosicion(animal, 2000);
        assertPosicion(695, 420, animal); // vuelve las 60 unidades hasta la zona 12
    }

    @Test
    void unAnimalMuertoDejaDeMoverse() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(535, 420), 0, 0, 1);
        animal.actualizacionEnergia(0);
        assertFalse(reglas.actualizacionPosicion(animal, 1000));
        assertFalse(animal.consultaEnMovimiento());
        assertPosicion(695, 420, animal);
    }

    @Test
    void variosAnimalesTerminanRepartidosYNoEncimados() {
        Jugador jugador = jugadorConAnimales(5, 695, 420);
        for (int i = 0; i < 5; i++) {
            reglas.inicioMovimiento(jugador.consultaManada().consultaAnimales().get(i), new Punto(535, 420), 0, i, 5);
        }
        Set<String> posiciones = new HashSet<>();
        for (Animal animal : jugador.consultaManada().consultaAnimales()) {
            reglas.actualizacionPosicion(animal, 20000);
            posiciones.add(animal.consultaPosicion().consultaPosicionX() + "," + animal.consultaPosicion().consultaPosicionY());
        }
        assertEquals(5, posiciones.size());
    }

    @Test
    void unSoloAnimalTerminaExactamenteEnElDestino() {
        Animal animal = primerAnimal(jugadorConAnimales(1, 695, 420));
        reglas.inicioMovimiento(animal, new Punto(575, 420), 0, 0, 1);
        reglas.actualizacionPosicion(animal, 20000);
        assertPosicion(575, 420, animal);
    }

    // ----- Validación de las órdenes -----

    @Test
    void unaOrdenDeLosPropiosAnimalesVivosEsValida() {
        Jugador jugador = jugadorConAnimales(3, 695, 420);
        assertTrue(reglas.validarMovimiento(jugador, jugador.consultaManada().consultaAnimales(), new Punto(500, 300)));
    }

    @Test
    void noSePuedenMoverAnimalesAjenos() {
        Jugador jugador = jugadorConAnimales(1, 695, 420);
        Jugador otro = jugadorConAnimales(1, 375, 240);
        assertFalse(reglas.validarMovimiento(jugador, otro.consultaManada().consultaAnimales(), new Punto(500, 300)));
    }

    @Test
    void noSePuedeMoverUnAnimalMuertoNiUnaListaVacia() {
        Jugador jugador = jugadorConAnimales(2, 695, 420);
        jugador.consultaManada().consultaAnimales().get(1).actualizacionEnergia(0);
        assertFalse(reglas.validarMovimiento(jugador, jugador.consultaManada().consultaAnimales(), new Punto(500, 300)));
        assertFalse(reglas.validarMovimiento(jugador, List.of(), new Punto(500, 300)));
    }

    @Test
    void elDestinoDebeEstarDentroDelMapa() {
        Jugador jugador = jugadorConAnimales(1, 695, 420);
        List<Animal> animales = jugador.consultaManada().consultaAnimales();
        assertFalse(reglas.validarMovimiento(jugador, animales, new Punto(-1, 100)));
        assertFalse(reglas.validarMovimiento(jugador, animales, new Punto(100, -1)));
        assertFalse(reglas.validarMovimiento(jugador, animales, new Punto(951, 100)));
        assertFalse(reglas.validarMovimiento(jugador, animales, new Punto(100, 551)));
        assertTrue(reglas.validarMovimiento(jugador, animales, new Punto(0, 0)));
        assertTrue(reglas.validarMovimiento(jugador, animales, new Punto(950, 550)));
    }

    @Test
    void unJugadorSinManadaNoPuedeMoverNada() {
        assertFalse(reglas.validarMovimiento(new Jugador("Sin manada", null), List.of(), new Punto(1, 1)));
    }

    @Test
    void unaOrdenDeMovimientoCancelaElAtaqueQueTuviera() {
        Jugador jugador = jugadorConAnimales(2, 695, 420);
        Animal atacante = jugador.consultaManada().consultaAnimales().get(0);
        atacante.ordenAtaque(jugador.consultaManada().consultaAnimales().get(1));
        reglas.inicioMovimiento(atacante, new Punto(535, 420), 0, 0, 1);
        org.junit.jupiter.api.Assertions.assertNull(atacante.consultaObjetivoAtaque());
    }
}
