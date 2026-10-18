package jungleattack.servidor.reglas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Camino;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.Taza;

//Reglas de los objetos de la partida: dónde se colocan las tazas y qué pasa cuando se consumen.
public class ReglasObjetos {

    //Crea las 10 tazas y las coloca en posiciones al azar sobre los caminos del mapa.
    public List<Taza> colocacionTazas(Mapa mapa, Random aleatorio) {
        List<Camino> caminos = mapa.consultaCaminos();
        List<Taza> tazas = new ArrayList<>();
        for (int i = 1; i <= ConstantesJuego.CANTIDAD_TAZAS; i++) {
            Camino camino = caminos.get(aleatorio.nextInt(caminos.size()));
            Punto posicion = camino.puntoEnFraccion(fraccionAleatoria(camino, aleatorio));
            tazas.add(new Taza(ConstantesJuego.PREFIJO_TAZA + ConstantesJuego.SEPARADOR_IDENTIFICADOR + i, posicion));
        }
        return tazas;
    }
    // Elige un punto del camino que quede al menos a la distancia minima de sus dos extremos.
    private double fraccionAleatoria(Camino camino, Random aleatorio) {
        double margen = ConstantesJuego.DISTANCIA_MINIMA_TAZA_ZONA / camino.consultaLongitud();
        if (margen >= 0.5) {
            return 0.5; // camino muy corto: la única opción es el punto medio
        }
        return margen + aleatorio.nextDouble() * (1 - 2 * margen);
    }

    public void consumoTaza(Animal animal, Taza taza) {
    }

    public void recuperacionPorEliminaciones(Manada manada) {
    }
}
