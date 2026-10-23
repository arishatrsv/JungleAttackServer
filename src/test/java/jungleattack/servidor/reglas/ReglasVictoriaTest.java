package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.List;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalJugador;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.ResumenPartida;
import org.junit.jupiter.api.Test;

class ReglasVictoriaTest {

    private final Mapa mapa = crearMapa();
    private final Jugador ana = jugador("Ana", Especie.JAGUAR);
    private final Jugador luis = jugador("Luis", Especie.TUCAN);
    // La bandera de Ana está en la zona 12 (695, 420) y la de Luis en la zona 4 (375, 240).
    private final List<Bandera> banderas = List.of(new Bandera(ana, mapa.zonaPorIdentificador(12)), new Bandera(luis, mapa.zonaPorIdentificador(4)));
    private final ReglasVictoria reglas = new ReglasVictoria(banderas, List.of(ana, luis));

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    private Jugador jugador(String apodo, Especie especie) {
        Jugador jugador = new Jugador(apodo, null);
        jugador.seleccionEspecie(especie);
        jugador.asignacionManada(new ManadaJugador(especie));
        return jugador;
    }

    private Animal animalDe(Jugador jugador, int x, int y) {
        Animal animal = new AnimalJugador("animal-" + jugador.consultaManada().consultaAnimales().size(), jugador.consultaEspecie(), 20, new Punto(x, y));
        jugador.consultaManada().incorporacionAnimal(animal);
        return animal;
    }

    @Test
    void quienTocaLaBanderaDelOponenteGana() {
        assertSame(luis, reglas.comprobacionVictoria(animalDe(luis, 695, 420))); // un animal de Luis en la bandera de Ana
        assertSame(ana, reglas.comprobacionVictoria(animalDe(ana, 375, 240))); // un animal de Ana en la bandera de Luis
    }

    @Test
    void tocarLaPropiaBanderaNoDaLaVictoria() {
        assertNull(reglas.comprobacionVictoria(animalDe(ana, 695, 420)));
        assertNull(reglas.comprobacionVictoria(animalDe(luis, 375, 240)));
    }

    @Test
    void lejosDeLasBanderasNoPasaNada() {
        assertNull(reglas.comprobacionVictoria(animalDe(ana, 535, 330)));
        assertNull(reglas.comprobacionVictoria(animalDe(luis, 215, 140)));
    }

    @Test
    void elResumenTraeLasEstadisticasDeCadaJugadorYLaDuracion() {
        ana.consultaManada().incorporacionAnimal(new AnimalJugador("jaguar-1", Especie.JAGUAR, 20, new Punto(1, 1)));
        ana.consultaManada().incorporacionAnimal(new AnimalJugador("jaguar-2", Especie.JAGUAR, 20, new Punto(2, 2)));
        ana.consultaManada().consultaAnimales().get(0).recepcionDanio(100); // Ana perdió un animal
        ana.consultaManada().registroBotEliminado();
        ana.consultaManada().registroBotEliminado();
        ana.consultaManada().consumoTaza();
        luis.consultaManada().consumoTaza();
        luis.consultaManada().consumoTaza();
        luis.consultaManada().consumoTaza();

        ResumenPartida resumen = reglas.creacionResumen(ana, ConstantesProtocolo.RAZON_BANDERA, 1000, 126500);

        assertSame(ana, resumen.consultaGanador());
        assertEquals(ConstantesProtocolo.RAZON_BANDERA, resumen.consultaRazon());
        assertEquals(125.5, resumen.consultaDuracion(), 0.0001);
        ResumenPartida.Estadisticas deAna = resumen.consultaEstadisticas(ana);
        assertEquals("Ana", deAna.consultaApodo());
        assertEquals(Especie.JAGUAR, deAna.consultaEspecie());
        assertEquals(2, deAna.consultaBotsEliminados());
        assertEquals(1, deAna.consultaAnimalesPerdidos());
        assertEquals(1, deAna.consultaTazasConsumidas());
        ResumenPartida.Estadisticas deLuis = resumen.consultaEstadisticas(luis);
        assertEquals(0, deLuis.consultaBotsEliminados());
        assertEquals(0, deLuis.consultaAnimalesPerdidos());
        assertEquals(3, deLuis.consultaTazasConsumidas());
    }

    @Test
    void laDuracionNuncaEsNegativa() {
        assertEquals(0.0, reglas.creacionResumen(ana, ConstantesProtocolo.RAZON_BANDERA, 5000, 1000).consultaDuracion(), 0.0001);
    }
}
