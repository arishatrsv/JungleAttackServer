package jungleattack.servidor.modelo;


import common.model.Especie;
import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;

//Animal individual dentro de una partida. Hereda (identificador,energia,vida,posicion y especie)
public class Animal extends common.model.Animal {
    //Manada a la que pertenece. De ella sale el daño que causa y la energía máxima que puede tener.
    private Manada manada;
    // Camino que está recorriendo. El primer punto es donde estaba al recibir la orden; avanza hacia ruta
    private List<Punto> ruta = List.of();
    private int indiceRuta;
    private boolean enMovimiento;
    // Posición con decimales.
    private double posicionPreciseX;
    private double posicionPreciseY;
    // Instante de la última vez que se actualizó su movimiento. Con él se calcula cuánto avanzó.
    private long ultimaActualizacion;
    private Animal objetivoAtaque;
    private Manada manada;

    public Animal(String identificador, Especie especie, int energia, Punto posicion) {
        super(identificador, especie, energia, posicion);
        this.posicionPreciseX = posicion.consultaPosicionX();
        this.posicionPreciseY = posicion.consultaPosicionY();
    }

    // La manada se asigna cuando el animal se incorpora a ella (ver Manada.incorporacionAnimal).
    public void asignacionManada(Manada manada) {
        this.manada = manada;
    }

    public Manada consultaManada() {
        return manada;
    }

    // Los bots lo sobrescriben para devolver true, así nadie tiene que preguntar por la clase concreta.
    public boolean esBot() {
        return false;
    }

    //Empieza a recorrer una ruta. El primer punto debe ser la posición actual del animal.
    public void asignacionRuta(List<Punto> ruta, long ahora) {
        this.ruta = List.copyOf(ruta);
        this.indiceRuta = 1;
        this.posicionPreciseX = consultaPosicion().consultaPosicionX();
        this.posicionPreciseY = consultaPosicion().consultaPosicionY();
        this.ultimaActualizacion = ahora;
        this.enMovimiento = ruta.size() > 1;
    }
    // Deja de moverse y borra la ruta.
    public void limpiezaRuta() {
        this.ruta = List.of();
        this.indiceRuta = 0;
        this.enMovimiento = false;
    }

    public List<Punto> consultaRuta() {
        return ruta;
    }

    public int consultaIndiceRuta() {
        return indiceRuta;
    }

    public void actualizacionIndiceRuta(int indiceRuta) {
        this.indiceRuta = indiceRuta;
    }

    public boolean consultaEnMovimiento() {
        return enMovimiento;
    }

    public double consultaPosicionPrecisaX() {
        return posicionPreciseX;
    }

    public double consultaPosicionPrecisaY() {
        return posicionPreciseY;
    }
    // Guarda la posición con decimales y deja la posición entera.
    public void actualizacionPosicionPrecisa(double x, double y) {
        this.posicionPreciseX = x;
        this.posicionPreciseY = y;
        actualizacionPosicion(new Punto((int) Math.round(x), (int) Math.round(y)));
    }
    
    public long consultaUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void actualizacionUltimaActualizacion(long ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public void recepcionDanio(int cantidad) {
    }

    public void muerte() {
    }

    public void ordenAtaque(Animal objetivo) {
    }

    public void detencionAtaque() {
    }
}
