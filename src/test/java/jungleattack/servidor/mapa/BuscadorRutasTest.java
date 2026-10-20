package jungleattack.servidor.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuscadorRutasTest {

    private final Mapa mapa = crearMapa();
    private final BuscadorRutas buscador = new BuscadorRutas(mapa);

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    private List<Integer> identificadores(List<Zona> zonas) {
        List<Integer> ids = new ArrayList<>();
        for (Zona zona : zonas) {
            ids.add(zona.consultaIdentificador());
        }
        return ids;
    }

    private double largo(List<Punto> ruta) {
        double total = 0;
        for (int i = 0; i + 1 < ruta.size(); i++) {
            total += Math.hypot(ruta.get(i + 1).consultaPosicionX() - ruta.get(i).consultaPosicionX(),
                    ruta.get(i + 1).consultaPosicionY() - ruta.get(i).consultaPosicionY());
        }
        return total;
    }

    private void assertPunto(int x, int y, Punto punto) {
        assertEquals(x, punto.consultaPosicionX());
        assertEquals(y, punto.consultaPosicionY());
    }

    // ----- Entre zonas -----

    @Test
    void laRutaEntreLaMismaZonaEsSoloEsaZona() {
        Zona zona = mapa.zonaPorIdentificador(12);
        assertEquals(List.of(12), identificadores(buscador.busquedaRuta(zona, zona)));
    }

    @Test
    void laRutaEntreZonasVecinasEsDirecta() {
        assertEquals(List.of(12, 11), identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(12), mapa.zonaPorIdentificador(11))));
    }

    @Test
    void laRutaAtraviesaLasZonasIntermedias() {
        assertEquals(List.of(12, 9, 6), identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(12), mapa.zonaPorIdentificador(6))));
        assertEquals(List.of(7, 4, 8, 11), identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(7), mapa.zonaPorIdentificador(11))));
    }

    @Test
    void laRutaEsLaMasCortaYNoLaQueTieneMenosZonas() {
        // De la zona 1 a la 4 hay dos opciones: 1-3-4 (160 + 100 = 260) o 1-2-7-4 (190 + 160 + 90 = 440).
        assertEquals(List.of(1, 3, 4), identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(1), mapa.zonaPorIdentificador(4))));
    }

    @Test
    void laRutaEsLaMismaEnLosDosSentidos() {
        List<Integer> ida = identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(7), mapa.zonaPorIdentificador(11)));
        List<Integer> vuelta = new ArrayList<>(identificadores(buscador.busquedaRuta(mapa.zonaPorIdentificador(11), mapa.zonaPorIdentificador(7))));
        java.util.Collections.reverse(vuelta);
        assertEquals(ida, vuelta);
    }

    // ----- Entre puntos -----

    @Test
    void entreLosCentrosDeDosZonasVecinasLaRutaSonLosDosPuntos() {
        List<Punto> ruta = buscador.busquedaRuta(new Punto(695, 420), new Punto(535, 420));
        assertEquals(2, ruta.size());
        assertPunto(695, 420, ruta.get(0));
        assertPunto(535, 420, ruta.get(1));
    }

    @Test
    void dentroDelMismoCaminoVaDerechoSinPasarPorLasZonas() {
        List<Punto> ruta = buscador.busquedaRuta(new Punto(575, 420), new Punto(655, 420));
        assertEquals(2, ruta.size());
        assertPunto(575, 420, ruta.get(0));
        assertPunto(655, 420, ruta.get(1));
        assertEquals(80.0, largo(ruta), 0.5);
    }

    @Test
    void dentroDelMismoCaminoFuncionaEnLosDosSentidos() {
        List<Punto> ruta = buscador.busquedaRuta(new Punto(655, 420), new Punto(575, 420));
        assertEquals(2, ruta.size());
        assertEquals(80.0, largo(ruta), 0.5);
    }

    @Test
    void desdeUnPuntoEnUnCaminoHastaOtraZonaPasaPorElExtremoMasConveniente() {
        // Desde la mitad del camino 11-12 hasta el centro de la zona 6: sale por la zona 12, sube a la 9 y llega a la 6.
        List<Punto> ruta = buscador.busquedaRuta(new Punto(615, 420), new Punto(695, 140));
        assertPunto(615, 420, ruta.get(0));
        assertPunto(695, 420, ruta.get(1));
        assertPunto(695, 240, ruta.get(2));
        assertPunto(695, 140, ruta.get(3));
        assertEquals(80 + 180 + 100, largo(ruta), 1.0);
    }

    @Test
    void haciaUnPuntoEnLaMitadDeUnCaminoLaRutaTerminaAhi() {
        // De la zona 7 a la mitad del camino 9-12 (695, 330): 7 -> 4 -> 8 -> 9 -> punto.
        List<Punto> ruta = buscador.busquedaRuta(new Punto(375, 330), new Punto(695, 330));
        assertPunto(375, 330, ruta.get(0));
        assertPunto(375, 240, ruta.get(1));
        assertPunto(535, 240, ruta.get(2));
        assertPunto(695, 240, ruta.get(3));
        assertPunto(695, 330, ruta.get(4));
        assertEquals(500.0, largo(ruta), 1.0);
    }

    @Test
    void unPuntoFueraDeLosCaminosSeLlevaAlPuntoMasCercanoDeLosCaminos() {
        // Un animal en formación, 30 unidades al lado de la zona 7, primero va al centro de la zona.
        List<Punto> ruta = buscador.busquedaRuta(new Punto(405, 330), new Punto(375, 240));
        assertPunto(405, 330, ruta.get(0));
        assertPunto(375, 330, ruta.get(1));
        assertPunto(375, 240, ruta.get(ruta.size() - 1));
    }

    @Test
    void unDestinoFueraDeLosCaminosTerminaEnElPuntoDeLosCaminosMasCercano() {
        // (800, 100) queda arriba del camino 5-6 (y = 140); el punto más cercano es (800, 140), a 40 unidades.
        List<Punto> ruta = buscador.busquedaRuta(new Punto(695, 140), new Punto(800, 100));
        Punto ultimo = ruta.get(ruta.size() - 1);
        assertTrue(ultimo.consultaPosicionY() == 140 && ultimo.consultaPosicionX() >= 695 && ultimo.consultaPosicionX() <= 800);
    }

    @Test
    void siYaEstaEnElDestinoLaRutaTieneUnSoloPunto() {
        List<Punto> ruta = buscador.busquedaRuta(new Punto(375, 240), new Punto(375, 240));
        assertEquals(1, ruta.size());
    }

    @Test
    void laRutaSiempreSeApoyaEnLosCaminosEntreCualquierParDeZonas() {
        for (Zona origen : mapa.consultaZonas()) {
            for (Zona destino : mapa.consultaZonas()) {
                List<Punto> ruta = buscador.busquedaRuta(origen.consultaPosicion(), destino.consultaPosicion());
                assertPunto(origen.consultaPosicion().consultaPosicionX(), origen.consultaPosicion().consultaPosicionY(), ruta.get(0));
                assertPunto(destino.consultaPosicion().consultaPosicionX(), destino.consultaPosicion().consultaPosicionY(), ruta.get(ruta.size() - 1));
            }
        }
    }
}
