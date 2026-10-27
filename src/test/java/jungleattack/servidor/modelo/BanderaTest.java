package jungleattack.servidor.modelo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.partidas.Jugador;
import org.junit.jupiter.api.Test;

class BanderaTest {

    // El radio sale de la constante, así las pruebas siguen valiendo si se cambia el valor.
    private static final int RADIO = ConstantesJuego.RADIO_BANDERA;

    private final Mapa mapa = crearMapa();
    private final Zona zona = mapa.zonaPorIdentificador(12); // centro (695, 420)
    private final Jugador dueno = jugador("Ana", Especie.JAGUAR);
    private final Jugador rival = jugador("Luis", Especie.TUCAN);
    private final Bandera bandera = new Bandera(dueno, zona);

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    private Jugador jugador(String apodo, Especie especie) {
        Jugador jugador = new Jugador(apodo, null);
        jugador.asignacionManada(new ManadaJugador(especie));
        return jugador;
    }

    private Animal animalDe(Jugador jugador, int x, int y) {
        Animal animal = new AnimalJugador("a-" + jugador.consultaManada().consultaAnimales().size(), jugador.consultaEspecie() == null ? Especie.JAGUAR : jugador.consultaEspecie(), 20, new Punto(x, y));
        jugador.consultaManada().incorporacionAnimal(animal);
        return animal;
    }

    @Test
    void laBanderaEstaEnElCentroDeLaZona() {
        assertTrue(bandera.consultaPosicion().consultaPosicionX() == 695 && bandera.consultaPosicion().consultaPosicionY() == 420);
    }

    @Test
    void unAnimalDelRivalEnElRadioLaToca() {
        assertTrue(bandera.deteccionContacto(animalDe(rival, 695, 420)));
        assertTrue(bandera.deteccionContacto(animalDe(rival, 695 + RADIO / 2, 420 + RADIO / 2))); // en diagonal, dentro del radio
    }

    @Test
    void elLimiteDelRadioCuentaComoTocar() {
        assertTrue(bandera.deteccionContacto(animalDe(rival, 695 + RADIO, 420))); // exactamente en el borde
        assertFalse(bandera.deteccionContacto(animalDe(rival, 695 + RADIO + 1, 420))); // un paso afuera
    }

    @Test
    void unAnimalDelRivalLejosNoLaToca() {
        assertFalse(bandera.deteccionContacto(animalDe(rival, 600, 420)));
    }

    @Test
    void losAnimalesDelPropioDuenoNoLaTocan() {
        assertFalse(bandera.deteccionContacto(animalDe(dueno, 695, 420)));
    }

    @Test
    void losBotsNoLaTocan() {
        ManadaBots bots = new ManadaBots(Especie.COCODRILO);
        AnimalBot bot = new AnimalBot("cocodrilo-1", Especie.COCODRILO, 10, new Punto(695, 420));
        bots.incorporacionAnimal(bot);
        assertFalse(bandera.deteccionContacto(bot));
    }

    @Test
    void unAnimalMuertoNoLaToca() {
        Animal muerto = animalDe(rival, 695, 420);
        muerto.recepcionDanio(100);
        assertFalse(bandera.deteccionContacto(muerto));
    }
}
