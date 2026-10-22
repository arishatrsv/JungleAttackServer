package jungleattack.servidor.reglas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Camino;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.ManadaJugador;
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

    //Un animal consume una taza: la taza desaparece y el daño de toda la manada del animal sube en 1.
    //La taza la consume solo el primer animal que la toque, aunque dos la toquen casi al mismo tiempo
    //Los bots no consumen tazas.
    public boolean consumoTaza(Animal animal, Taza taza) {
        if (animal.esBot() || !animal.consultaVida() || !(animal.consultaManada() instanceof ManadaJugador manada)) {
            return false;
        }
        if (!taza.consumo()) {
            return false;
        }
        manada.consumoTaza();
        return true;
    }

    //Revisa si un animal que acaba de moverse tocó alguna taza. Las tazas consumidas se sacan de su zona para que dejen de verse
    //Se revisan todas las tazas de la partida (son solo 10) y no solo las de la zona del animal, porque una
    //taza cerca del borde de una zona puede quedar a pocos pasos de un animal que está en la zona vecina.
    public int recogidaTazas(Animal animal, Collection<Taza> tazas, Mapa mapa) {
        if (animal.esBot() || !animal.consultaVida()) {
            return 0;
        }
        double x;
        double y;
        synchronized (animal) { // otro hilo podría estar moviendo al animal
            x = animal.consultaPosicion().consultaPosicionX();
            y = animal.consultaPosicion().consultaPosicionY();
        }
        int consumidas = 0;
        for (Taza taza : tazas) {
            if (taza.consultaConsumida()) {
                continue;
            }
            double distancia = Math.hypot(taza.consultaPosicion().consultaPosicionX() - x,
                    taza.consultaPosicion().consultaPosicionY() - y);
            if (distancia <= ConstantesJuego.RADIO_RECOGIDA_TAZA && consumoTaza(animal, taza)) {
                mapa.zonaPorPosicion(taza.consultaPosicion()).eliminacionTaza(taza);
                consumidas++;
            }
        }
        return consumidas; //cuántas tazas consumió el animal en esta revisión
    }

    //Cada 2 bots eliminados, todos los animales vivos de la manada ganan 1 de energía,
    //Se llama cada vez que la manada elimina un bot
    public boolean recuperacionPorEliminaciones(ManadaJugador manada, int totalEliminados) {
        if (totalEliminados <= 0 || totalEliminados % ConstantesJuego.ELIMINACIONES_POR_ENERGIA != 0) {
            return false;
        }
        manada.recuperacionEnergia();
        return true;
    }
}
