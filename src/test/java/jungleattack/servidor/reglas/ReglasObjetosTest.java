package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Camino;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.modelo.Taza;
import org.junit.jupiter.api.Test;

class ReglasObjetosTest {

    @Test
    void colocaLaCantidadDeTazasConIdentificadoresUnicos() {
        List<Taza> tazas = new ReglasObjetos().colocacionTazas(crearMapa(), new Random(42));
        Set<String> identificadores = new HashSet<>();

        for (Taza taza : tazas) {
            identificadores.add(taza.consultaIdentificador());
        }

        assertEquals(ConstantesJuego.CANTIDAD_TAZAS, tazas.size());
        assertEquals(tazas.size(), identificadores.size());
    }

    @Test
    void colocaLasTazasSobreCaminosYLejosDeSusExtremos() {
        Mapa mapa = crearMapa();
        List<Taza> tazas = new ReglasObjetos().colocacionTazas(mapa, new Random(42));

        for (Taza taza : tazas) {
            boolean sobreCaminoValido = mapa.consultaCaminos().stream()
                    .anyMatch(camino -> cumpleUbicacion(camino, taza));
            assertTrue(sobreCaminoValido, "La taza debe quedar en un camino y separada de sus extremos");
        }
    }

    private Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    private boolean cumpleUbicacion(Camino camino, Taza taza) {
        common.model.Punto[] extremos = {
            camino.consultaExtremos()[0].consultaPosicion(),
            camino.consultaExtremos()[1].consultaPosicion()
        };
        common.model.Punto posicion = taza.consultaPosicion();
        double distanciaInicio = distancia(posicion, extremos[0]);
        double distanciaFin = distancia(posicion, extremos[1]);

        return distanciaAlSegmento(posicion, extremos[0], extremos[1]) <= 1.0
                && distanciaInicio >= ConstantesJuego.DISTANCIA_MINIMA_TAZA_ZONA - 1.0
                && distanciaFin >= ConstantesJuego.DISTANCIA_MINIMA_TAZA_ZONA - 1.0;
    }

    private double distanciaAlSegmento(common.model.Punto punto,
            common.model.Punto inicio, common.model.Punto fin) {
        double dx = fin.consultaPosicionX() - inicio.consultaPosicionX();
        double dy = fin.consultaPosicionY() - inicio.consultaPosicionY();
        double longitudCuadrada = dx * dx + dy * dy;
        double fraccion = ((punto.consultaPosicionX() - inicio.consultaPosicionX()) * dx
                + (punto.consultaPosicionY() - inicio.consultaPosicionY()) * dy) / longitudCuadrada;
        fraccion = Math.max(0.0, Math.min(1.0, fraccion));
        double x = inicio.consultaPosicionX() + fraccion * dx;
        double y = inicio.consultaPosicionY() + fraccion * dy;
        return Math.hypot(punto.consultaPosicionX() - x, punto.consultaPosicionY() - y);
    }

    private double distancia(common.model.Punto primero, common.model.Punto segundo) {
        return Math.hypot(primero.consultaPosicionX() - segundo.consultaPosicionX(),
                primero.consultaPosicionY() - segundo.consultaPosicionY());
    }
}
