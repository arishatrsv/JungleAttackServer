package jungleattack.servidor.mapa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.constantes.ConstantesComunes;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jungleattack.servidor.modelo.Animal;

//El mapa de una partida: 12 zonas unidas por 15 caminos
public class Mapa {
    private final List<Zona> zonas = new ArrayList<>();
    private final List<Camino> caminos = new ArrayList<>();
    private int ancho;
    private int alto;

    //Lee mapa de los recursos de common y arma las zonas y los caminos
    public void generacionMapa() {
        InputStream entrada = Mapa.class.getClassLoader().getResourceAsStream(ConstantesComunes.RECURSO_MAPA);
        if (entrada == null) {
            throw new IllegalStateException("No se encontró " + ConstantesComunes.RECURSO_MAPA + " en los recursos");
        }
        try (Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            generacionMapa(lector);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el mapa", e);
        }
    }

    //Arma las zonas y los caminos a partir de un texto JSON con el formato de mapa
    public void generacionMapa(Reader lector) {
        zonas.clear(); // Vacía las zonas de cualquier mapa cargado anteriormente.
        caminos.clear(); // Vacía los caminos anteriores para evitar conservar datos viejos.
        JsonObject raiz = JsonParser.parseReader(lector).getAsJsonObject(); // Lee el JSON y obtiene su objeto principal.
        ancho = raiz.get(ConstantesComunes.MAPA_ANCHO).getAsInt(); // Lee del JSON el ancho del mapa.
        alto = raiz.get(ConstantesComunes.MAPA_ALTO).getAsInt(); // Lee del JSON el alto del mapa.
        for (JsonElement elemento : raiz.getAsJsonArray(ConstantesComunes.MAPA_ZONAS)) { // Recorre cada zona descrita en el JSON.
            zonas.add(lecturaZona(elemento.getAsJsonObject())); // Convierte el elemento JSON en una zona y la agrega al mapa.
        } 
        // Los caminos se arman después de las zonas, porque un camino necesita que sus dos zonas ya existan.
        Map<Integer, List<common.model.Camino>> conexiones = new HashMap<>(); // Prepara un índice que asocia cada zona con sus caminos.
        for (Zona zona : zonas) { // Recorre las zonas que ya se cargaron.
            conexiones.put(zona.consultaIdentificador(), new ArrayList<>()); // Crea una lista vacía de caminos para esta zona.
        } // Termina de preparar las listas de conexiones.
        for (JsonElement elemento : raiz.getAsJsonArray(ConstantesComunes.MAPA_CAMINOS)) { // Recorre cada camino descrito en el JSON.
            Camino camino = lecturaCamino(elemento.getAsJsonObject()); // Convierte el elemento JSON en un objeto camino.
            caminos.add(camino); // Guarda el camino en la lista general del mapa.
            for (common.model.Zona extremo : camino.consultaExtremos()) { // Recorre las dos zonas que conecta el camino.
                conexiones.get(extremo.consultaIdentificador()).add(camino); // Registra el camino como conexión de esta zona.
            } 
        } // Termina de cargar todos los caminos y asociarlos con sus zonas.
        for (Zona zona : zonas) { // Recorre las zonas para guardar en cada una sus caminos conectados.
            zona.actualizacionInformacion(zona.consultaPosicion(), conexiones.get(zona.consultaIdentificador())); 
            // Actualiza la información de la zona conservando su posición y asignándole sus conexiones.
        } 
    }

    private Zona lecturaZona(JsonObject datos) {
        int identificador = datos.get(ConstantesComunes.ZONA_ID).getAsInt();
        String nombre = datos.get(ConstantesComunes.ZONA_NOMBRE).getAsString();
        Punto centro = new Punto(datos.get(ConstantesComunes.ZONA_X).getAsInt(),
                datos.get(ConstantesComunes.ZONA_Y).getAsInt());
        List<Punto> poligono = new ArrayList<>();
        JsonArray vertices = datos.getAsJsonArray(ConstantesComunes.ZONA_POLIGONO);
        for (JsonElement vertice : vertices) {
            JsonArray coordenadas = vertice.getAsJsonArray();
            poligono.add(new Punto(coordenadas.get(0).getAsInt(), coordenadas.get(1).getAsInt()));
        }
        return new Zona(identificador, nombre, centro, poligono);
    }

    private Camino lecturaCamino(JsonObject datos) {
        Zona desde = zonaPorIdentificador(datos.get(ConstantesComunes.CAMINO_DESDE).getAsInt());
        Zona hasta = zonaPorIdentificador(datos.get(ConstantesComunes.CAMINO_HASTA).getAsInt());
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Un camino une una zona que no existe");
        }
        return new Camino(datos.get(ConstantesComunes.CAMINO_ID).getAsInt(), desde, hasta,
                datos.get(ConstantesComunes.CAMINO_LONGITUD).getAsDouble());
    }

    // Devuelve la zona con ese número, o null si no existe.
    public Zona zonaPorIdentificador(int identificador) {
        for (Zona zona : zonas) {
            if (zona.consultaIdentificador() == identificador) {
                return zona;
            }
        }
        return null;
    }
    //Devuelve las zonas que se pueden alcanzar directamente desde una zona, por un camino
    public List<Zona> zonasConectadas(Zona zona) {
        List<Zona> vecinas = new ArrayList<>();
        for (common.model.Camino conexion : zona.consultaConexiones()) {
            Zona otra = ((Camino) conexion).zonaOpuesta(zona);
            if (otra != null) {
                vecinas.add(otra);
            }
        }
        return vecinas;
    }

    //Devuelve la zona donde está un punto del mapa. 
    //Si el punto cae justo en el borde entre zonas devuelve la zona cuyo centro esté más cerca.
    public Zona zonaPorPosicion(common.model.Punto posicion) {
        Zona masCercana = null;
        double menorDistancia = Double.MAX_VALUE;
        for (Zona zona : zonas) {
            if (zona.contienePunto(posicion)) {
                return zona;
            }
            double distancia = zona.distanciaAlCentro(posicion);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                masCercana = zona;
            }
        }
        return masCercana;
    }

    //Pone al animal en la zona que le corresponde según su posición y lo saca de alguna otra
    //para que ninguna zona guarde un animal que ya no está y actualice
    public void actualizacionUbicacionAnimal(Animal animal) {
        Zona destino = zonaPorPosicion(animal.consultaPosicion());
        if (destino != null) {
            destino.registroAnimal(animal);
        }
        for (Zona zona : zonas) {
            if (zona != destino) {
                zona.eliminacionAnimal(animal);
            }
        }
    }

    public List<Zona> consultaZonas() {
        return Collections.unmodifiableList(zonas);
    }

    public List<Camino> consultaCaminos() {
        return Collections.unmodifiableList(caminos);
    }

    public int consultaAncho() {
        return ancho;
    }

    public int consultaAlto() {
        return alto;
    }
}
