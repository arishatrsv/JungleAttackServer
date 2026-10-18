package jungleattack.servidor.mapa;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Taza;

//Zona del mapa. Hereda 
//Las listas son CopyOnWriteArrayList porque cada zona la actualiza su propio hilo mientras
//otros hilos la consultan
public class Zona extends common.model.Zona {
    private final String nombre;

    //Vértices del contorno de la zona. 
    //Sirve para saber si un punto del mapa está dentro de ella
    private final List<Punto> poligono;

    private final List<Animal> animalesPresentes = new CopyOnWriteArrayList<>(); //Las listas son CopyOnWriteArrayList 
    private final List<Taza> tazasPresentes = new CopyOnWriteArrayList<>(); //porque cada zona actualiza su propio hilo mientras otros hilos la consultan

    public Zona(int identificador, String nombre, Punto centro, List<Punto> poligono) {
        super(identificador, centro);
        this.nombre = nombre;
        this.poligono = List.copyOf(poligono);
    }

    public String consultaNombre() {
        return nombre;
    }

    public List<Punto> consultaPoligono() {
        return poligono;
    }

    // Determina si el punto cae dentro del polígono que representa esta zona.
    public boolean contienePunto(common.model.Punto punto) { 
        double x = punto.consultaPosicionX(); // Obtiene la coordenada horizontal del punto que se quiere comprobar.
        double y = punto.consultaPosicionY(); // Obtiene la coordenada vertical del punto que se quiere comprobar.
        boolean dentro = false; // Inicializa el resultado; cada cruce del contorno invertirá este valor.
        for (int i = 0, anterior = poligono.size() - 1; i < poligono.size(); anterior = i++) { // Recorre los lados del polígono, tomando cada vértice y el anterior; el primero se conecta con el último.
            double xi = poligono.get(i).consultaPosicionX(); //coordenada horizontal del vértice actual.
            double yi = poligono.get(i).consultaPosicionY(); //coordenada vertical del vértice actual.
            double xa = poligono.get(anterior).consultaPosicionX(); // coordenada horizontal del vértice anterior.
            double ya = poligono.get(anterior).consultaPosicionY(); // coordenada vertical del vértice anterior.
            boolean cruzaLaAltura = (yi > y) != (ya > y); // Comprueba si el lado atraviesa la altura del punto.
            if (cruzaLaAltura && x < (xa - xi) * (y - yi) / (ya - yi) + xi) { // Calcula dónde cruza el lado esa altura y comprueba si el cruce queda a la derecha del punto.
                dentro = !dentro; // Invierte el resultado: un número impar de cruces significa que el punto está dentro.
            } // Si no hay cruce a la derecha, conserva el resultado actual.
        } 
        return dentro; // Devuelve si el punto quedó dentro según la paridad de cruces del contorno.
    }

    //Calcula la distancia en línea recta entre el punto recibido y el centro de esta zona.
    public double distanciaAlCentro(common.model.Punto punto) { 
        common.model.Punto centro = consultaPosicion(); // Obtiene las coordenadas del centro de la zona.
        return Math.hypot(punto.consultaPosicionX() - centro.consultaPosicionX(), // Calcula la diferencia horizontal entre el punto y el centro.
                punto.consultaPosicionY() - centro.consultaPosicionY()); // Math.hypot aplica Pitágoras: raíz de (diferencia horizontal² + diferencia vertical²).
    }

    // Registra un animal en la zona.
    public void registroAnimal(Animal animal) {
        if (!animalesPresentes.contains(animal)) {
            animalesPresentes.add(animal);
        }
    }

   public void eliminacionAnimal(Animal animal) {
        animalesPresentes.remove(animal);
    }

    public void registroTaza(Taza taza) {
        if (!tazasPresentes.contains(taza)) {
            tazasPresentes.add(taza);
        }
    }

    public void eliminacionTaza(Taza taza) {
        tazasPresentes.remove(taza);
    }

    // Copia de los animales que están en la zona.
    public List<Animal> animalesEnZona() {
        return new ArrayList<>(animalesPresentes);
    }

    // Copia de las tazas que están en la zona.
    public List<Taza> tazasEnZona() {
        return new ArrayList<>(tazasPresentes);
    }
}
