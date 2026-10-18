package jungleattack.servidor.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import jungleattack.servidor.modelo.Animal;
import org.junit.jupiter.api.Test;

class MapaTest {

    // Carga el mapa real (mapa.json de common), igual que lo hace el servidor.
    private Mapa mapaReal() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    @Test
    void elMapaTieneDoceZonasQuinceCaminosYSuTamano() {
        Mapa mapa = mapaReal();
        assertEquals(12, mapa.consultaZonas().size());
        assertEquals(15, mapa.consultaCaminos().size());
        assertEquals(950, mapa.consultaAncho());
        assertEquals(550, mapa.consultaAlto());
    }

    @Test
    void cadaCaminoQuedaRegistradoEnLasDosZonasQueUne() {
        Mapa mapa = mapaReal();
        int conexiones = 0;
        for (Zona zona : mapa.consultaZonas()) {
            assertTrue(zona.consultaConexiones().size() >= 1, "la zona " + zona.consultaIdentificador() + " está aislada");
            conexiones += zona.consultaConexiones().size();
        }
        assertEquals(30, conexiones); // 15 caminos, cada uno cuenta en sus dos extremos
    }

    @Test
    void laZonaUnoSeConectaConLasZonasDosYTres() {
        Mapa mapa = mapaReal();
        Set<Integer> vecinas = new HashSet<>();
        for (Zona vecina : mapa.zonasConectadas(mapa.zonaPorIdentificador(1))) {
            vecinas.add(vecina.consultaIdentificador());
        }
        assertEquals(Set.of(2, 3), vecinas);
    }

    @Test
    void sePuedeBuscarUnaZonaPorSuNumero() {
        Mapa mapa = mapaReal();
        assertEquals("Monteverde", mapa.zonaPorIdentificador(4).consultaNombre());
        assertNull(mapa.zonaPorIdentificador(0));
        assertNull(mapa.zonaPorIdentificador(13));
    }

    @Test
    void elCentroDeCadaZonaEstaDentroDeSuPoligono() {
        Mapa mapa = mapaReal();
        for (Zona zona : mapa.consultaZonas()) {
            assertTrue(zona.contienePunto(zona.consultaPosicion()), "centro fuera de la zona " + zona.consultaIdentificador());
            assertSame(zona, mapa.zonaPorPosicion(zona.consultaPosicion()));
        }
    }

    @Test
    void todoPuntoDeUnCaminoCaeDentroDeUnaZona() {
        Mapa mapa = mapaReal();
        for (Camino camino : mapa.consultaCaminos()) {
            for (int paso = 0; paso <= 20; paso++) {
                Punto punto = camino.puntoEnFraccion(paso / 20.0);
                Zona zona = mapa.zonaPorPosicion(punto);
                assertNotNull(zona);
                assertTrue(zona.contienePunto(punto), "punto del camino " + camino.consultaIdentificador() + " sin zona");
            }
        }
    }

    @Test
    void unPuntoFueraDeTodasLasZonasSeAsignaALaMasCercana() {
        Mapa mapa = mapaReal();
        Punto lejos = new Punto(0, 0);
        for (Zona zona : mapa.consultaZonas()) {
            assertFalse(zona.contienePunto(lejos));
        }
        assertEquals(1, mapa.zonaPorPosicion(lejos).consultaIdentificador());
    }

    @Test
    void puntoEnFraccionDevuelveLosExtremosYElMedioDelCamino() {
        Mapa mapa = mapaReal();
        Camino camino = mapa.consultaCaminos().get(0); // une la zona 1 (215,140) con la zona 2 (215,330)
        assertEquals(215, camino.puntoEnFraccion(0).consultaPosicionX());
        assertEquals(140, camino.puntoEnFraccion(0).consultaPosicionY());
        assertEquals(330, camino.puntoEnFraccion(1).consultaPosicionY());
        assertEquals(235, camino.puntoEnFraccion(0.5).consultaPosicionY());
    }

    @Test
    void laZonaOpuestaEsLaDelOtroExtremoDelCamino() {
        Mapa mapa = mapaReal();
        Camino camino = mapa.consultaCaminos().get(0);
        Zona uno = mapa.zonaPorIdentificador(1);
        Zona dos = mapa.zonaPorIdentificador(2);
        assertSame(dos, camino.zonaOpuesta(uno));
        assertSame(uno, camino.zonaOpuesta(dos));
        assertNull(camino.zonaOpuesta(mapa.zonaPorIdentificador(7)));
    }

    @Test
    void alMoverUnAnimalDeZonaLasListasDeLasZonasSeActualizan() {
        Mapa mapa = mapaReal();
        Zona origen = mapa.zonaPorIdentificador(1);
        Zona destino = mapa.zonaPorIdentificador(3);
        Animal animal = new Animal("jaguar-1", Especie.JAGUAR, 20,
                new Punto(origen.consultaPosicion().consultaPosicionX(), origen.consultaPosicion().consultaPosicionY()));
        mapa.actualizacionUbicacionAnimal(animal);
        assertEquals(1, origen.animalesEnZona().size());

        animal.actualizacionPosicion(destino.consultaPosicion());
        mapa.actualizacionUbicacionAnimal(animal);
        assertEquals(0, origen.animalesEnZona().size());
        assertEquals(1, destino.animalesEnZona().size());
    }

    @Test
    void unAnimalNoQuedaRepetidoEnLaZona() {
        Mapa mapa = mapaReal();
        Zona zona = mapa.zonaPorIdentificador(5);
        Animal animal = new Animal("tucan-1", Especie.TUCAN, 10, new Punto(535, 140));
        zona.registroAnimal(animal);
        zona.registroAnimal(animal);
        assertEquals(1, zona.animalesEnZona().size());
    }

    @Test
    void sePuedeArmarUnMapaPropioDesdeUnTextoJson() {
        String json = "{\"ancho\":100,\"alto\":50,\"zonas\":["
                + "{\"id\":1,\"nombre\":\"A\",\"x\":10,\"y\":10,\"poligono\":[[0,0],[20,0],[20,20],[0,20]]},"
                + "{\"id\":2,\"nombre\":\"B\",\"x\":60,\"y\":10,\"poligono\":[[40,0],[80,0],[80,20],[40,20]]}],"
                + "\"caminos\":[{\"id\":1,\"desde\":1,\"hasta\":2,\"longitud\":50}]}";
        Mapa mapa = new Mapa();
        mapa.generacionMapa(new StringReader(json));
        assertEquals(2, mapa.consultaZonas().size());
        assertEquals(1, mapa.consultaCaminos().size());
        assertEquals(50.0, mapa.consultaCaminos().get(0).consultaLongitud(), 0.001);
        assertEquals(2, mapa.zonasConectadas(mapa.zonaPorIdentificador(1)).get(0).consultaIdentificador());
    }

    @Test
    void unCaminoQueUneUnaZonaInexistenteEsUnError() {
        String json = "{\"ancho\":100,\"alto\":50,\"zonas\":["
                + "{\"id\":1,\"nombre\":\"A\",\"x\":10,\"y\":10,\"poligono\":[[0,0],[20,0],[20,20],[0,20]]}],"
                + "\"caminos\":[{\"id\":1,\"desde\":1,\"hasta\":9,\"longitud\":50}]}";
        assertThrows(IllegalArgumentException.class, () -> new Mapa().generacionMapa(new StringReader(json)));
    }
}
