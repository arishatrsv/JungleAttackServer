package jungleattack.servidor.mapa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

//Calcula rutas por el grafo del mapa con el algoritmo de Dijkstra
//Hay dos versiones: entre zonas (busca qué zonas hay que cruzar) y entre puntos (devuelve los puntos
//exactos que hay que recorrer, porque un animal puede estar o ir a cualquier punto de un camino).
public class BuscadorRutas {
    private final Mapa mapa;
    public BuscadorRutas(Mapa mapa){
        this.mapa = mapa;
    }

    //Ruta más corta entre dos zonas la de origen y la destino.
    public List<Zona> busquedaRuta(Zona origen, Zona destino) { 
       Map<Object, Map<Object, Double>> grafo = grafoDeZonas(); //Obtiene el grafo que representa las conexiones
       //Ejecuta el algoritmo Dijkstra para encontrar la ruta
        List<Object> nodos = dijkstra(grafo, origen.consultaIdentificador(), destino.consultaIdentificador()); 
        List<Zona> ruta = new ArrayList<>(); //Crea una lista vacía para almacenar las zonas
        for (Object nodo : nodos) {
            //Busca la zona y la agrega a la lista de la ruta
            ruta.add(mapa.zonaPorIdentificador((Integer) nodo));
        }
        return ruta; //devuelve la lista de zonas que componen la ruta
    }

    //Ruta más corta entre dos puntos, siempre por los caminos.
    public List<Punto> busquedaRutaList(common.model.Punto origen, common.model.Punto destino){
        PuntoEnCamino inicio = puntoMasCercano(origen);
        PuntoEnCamino fin = puntoMasCercano(destino);

        //Se arma el grafo de zonas y se parten los caminos donde caen el inicio y el fin
        //los nodos son Integer(zonas) o String (los virtuales)
        final String nodoInicio = "Inicio";
        final String nodoFin = "Fin";
        Map<Object, Map<Object, Double>> grafo = new HashMap<>();
        Map<Object, Punto> coordenadas = new HashMap<>();
        for (Zona zona : mapa.consultaZonas()) {
            grafo.put(zona.consultaIdentificador(), new HashMap<>());
            coordenadas.put(zona.consultaIdentificador(), copia(zona.consultaPosicion()));
        }
        coordenadas.put(nodoInicio, inicio.punto);
        coordenadas.put(nodoFin, fin.punto);

        for (Camino camino : mapa.consultaCaminos()) {
            // Los nodos de este camino, en orden desde su zona de inicio (fracción 0) hasta la de destino (fracción 1).
            List<Object> nodos = new ArrayList<>(); //guarda los identificadores de los nodos
            List<Double> fracciones = new ArrayList<>(); //guarda la posición de cada nodo
            common.model.Zona[] extremos = camino.consultaExtremos(); //obtiene las dos zonas que representan los extremos
            nodos.add(extremos[0].consultaIdentificador()); //identificar zona inicial
            fracciones.add(0.0);
            List<Object[]> virtuales = new ArrayList<>(); //guarda nodos virtuales
            if (inicio.camino == camino) {
                virtuales.add(new Object[] {nodoInicio, inicio.fraccion}); //Guarda el identificador del nodo virtual de destino
            }
            if (fin.camino == camino) {
                virtuales.add(new Object[] {nodoFin, fin.fraccion});// ordena los nodos virtuales según posición en el camino
            }
            virtuales.sort((a, b) -> Double.compare((Double) a[1], (Double) b[1]));
            for (Object[] virtual : virtuales) {
                nodos.add(virtual[0]);  //Agrega el identificador del nodo virtual a la lista de nodo
                fracciones.add((Double) virtual[1]);  //Agrega la posición del nodo virtual 
            }
            nodos.add(extremos[1].consultaIdentificador()); //Agrega el identificador de la zona final del camino.
            fracciones.add(1.0); //indica que la zona final se encuentra en la posicion (1.0)
            for (int i = 0; i + 1 < nodos.size(); i++) { //recorre para crear conexiones entre ellos
                // Calcula la longitud del tramo entre dos nodos consecutivos,
                // usando la diferencia de sus posiciones y la longitud del camino.
                double largo = (fracciones.get(i + 1) - fracciones.get(i)) * camino.consultaLongitud();
                conexion(grafo, nodos.get(i), nodos.get(i + 1), largo);
            }
        }
        List<Object> nodosRuta = dijkstra(grafo, nodoInicio, nodoFin); 
        List<Punto> ruta = new ArrayList<>(); //crea la lista de puntos de la ruta
        agregarSinRepetir(ruta, copia(origen)); //agrega el punto de origen sin repetirlo
        for (Object nodo : nodosRuta) { //Agrega las coordenadas de cada nodo de la ruta sin repetir puntos
            agregarSinRepetir(ruta, coordenadas.get(nodo));
        }
        return ruta; //devuelve la ruta encontrada
    }

    // Grafo con una arista por cada camino (en los dos sentidos), cuyo costo es su longitud.
    private Map<Object, Map<Object, Double>> grafoDeZonas() {
        Map<Object, Map<Object, Double>> grafo = new HashMap<>();
        for (Zona zona : mapa.consultaZonas()) {
            grafo.put(zona.consultaIdentificador(), new HashMap<>());
        }
        for (Camino camino : mapa.consultaCaminos()) {
            common.model.Zona[] extremos = camino.consultaExtremos();
            conexion(grafo, extremos[0].consultaIdentificador(), extremos[1].consultaIdentificador(), camino.consultaLongitud());
        }
        return grafo;
    }

    private void conexion(Map<Object, Map<Object, Double>> grafo, Object a, Object b, double costo) {
        grafo.computeIfAbsent(a, k -> new HashMap<>()).put(b, costo);
        grafo.computeIfAbsent(b, k -> new HashMap<>()).put(a, costo);
    }

    //Dijkstra: va sacando siempre el nodo pendiente con menor distancia acumulada y mejora la distancia de sus vecinos.
    private List<Object> dijkstra(Map<Object, Map<Object, Double>> grafo, Object origen, Object destino) {
        Map<Object, Double> distancia = new HashMap<>();
        Map<Object, Object> anterior = new HashMap<>();
        PriorityQueue<Object[]> pendientes = new PriorityQueue<>((a, b) -> Double.compare((Double) a[1], (Double) b[1]));
        distancia.put(origen, 0.0);
        pendientes.add(new Object[] {origen, 0.0});
        while (!pendientes.isEmpty()) {
            Object[] actual = pendientes.poll();
            Object nodo = actual[0];
            double acumulada = (Double) actual[1];
            if (acumulada > distancia.getOrDefault(nodo, Double.MAX_VALUE)) {
                continue; // ya se encontró una forma más corta de llegar a este nodo
            }
            if (nodo.equals(destino)) {
                break;
            }
            for (Map.Entry<Object, Double> vecino : grafo.getOrDefault(nodo, Map.of()).entrySet()) {
                double nueva = acumulada + vecino.getValue();
                if (nueva < distancia.getOrDefault(vecino.getKey(), Double.MAX_VALUE)) {
                    distancia.put(vecino.getKey(), nueva);
                    anterior.put(vecino.getKey(), nodo);
                    pendientes.add(new Object[] {vecino.getKey(), nueva});
                }
            }
        }
        List<Object> ruta = new ArrayList<>();
        if (!distancia.containsKey(destino)) {
            return ruta;
        }
        for (Object nodo = destino; nodo != null; nodo = anterior.get(nodo)) {
            ruta.add(nodo);
        }
        Collections.reverse(ruta);
        return ruta;
    } 
    // Punto de los caminos más cercano a un punto cualquiera (puede ser un extremo, o sea el centro de una zona).
    private PuntoEnCamino puntoMasCercano(common.model.Punto punto) {
        PuntoEnCamino mejor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (Camino camino : mapa.consultaCaminos()) {
            common.model.Zona[] extremos = camino.consultaExtremos();
            double x1 = extremos[0].consultaPosicion().consultaPosicionX();
            double y1 = extremos[0].consultaPosicion().consultaPosicionY();
            double dx = extremos[1].consultaPosicion().consultaPosicionX() - x1;
            double dy = extremos[1].consultaPosicion().consultaPosicionY() - y1;
            // Proyección del punto sobre la línea del camino, limitada al tramo entre sus dos extremos.
            double fraccion = ((punto.consultaPosicionX() - x1) * dx + (punto.consultaPosicionY() - y1) * dy) / (dx * dx + dy * dy);
            fraccion = Math.max(0, Math.min(1, fraccion));
            double distancia = Math.hypot(punto.consultaPosicionX() - (x1 + fraccion * dx), punto.consultaPosicionY() - (y1 + fraccion * dy));
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejor = new PuntoEnCamino(camino, fraccion, camino.puntoEnFraccion(fraccion));
            }
        }
        return mejor;
    }
    
    private void agregarSinRepetir(List<Punto> ruta, Punto punto) {
        if (ruta.isEmpty() || !mismoPunto(ruta.get(ruta.size() - 1), punto)) {
            ruta.add(punto);
        }
    }

    private boolean mismoPunto(Punto a, Punto b) {
        return a.consultaPosicionX() == b.consultaPosicionX() && a.consultaPosicionY() == b.consultaPosicionY();
    }

    private Punto copia(common.model.Punto punto) {
        return new Punto(punto.consultaPosicionX(), punto.consultaPosicionY());
    }

    // Un punto sobre un camino: el camino, qué fracción de su largo y el punto resultante.
    private static class PuntoEnCamino {
        private final Camino camino;
        private final double fraccion;
        private final Punto punto;

        private PuntoEnCamino(Camino camino, double fraccion, Punto punto) {
            this.camino = camino;
            this.fraccion = fraccion;
            this.punto = punto;
        }
    }
}

