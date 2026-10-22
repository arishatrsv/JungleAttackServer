package jungleattack.servidor.modelo;

import common.model.Especie;
import java.util.List;
import jungleattack.servidor.mapa.Punto;

//Animal individual dentro de una partida. Hereda (identificador,energia,vida,posicion y especie)
public class Animal extends common.model.Animal {
    //Manada a la que pertenece. De ella sale el daño que causa y la energía máxima que puede tener.
    private Manada manada;
    //Camino que está recorriendo. El primer punto es donde estaba al recibir la orden; avanza hacia ruta
    private List<Punto> ruta = List.of();
    private int indiceRuta;
    private boolean enMovimiento;
    //Posición con decimales.
    private double posicionPreciseX;
    private double posicionPreciseY;
    //Instante de la última vez que se actualizó su movimiento. Con él se calcula cuánto avanzó.
    private long ultimaActualizacion;
    //A quién está atacando por orden del jugador. Los bots eligen su objetivo solos en cada ciclo.
    private Animal objetivoAtaque;
    //Instante del último ataque que hizo. Empieza muy atrás para que el primer ataque sea inmediato.
    private long ultimoAtaque = Long.MIN_VALUE / 2;

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

    //Resta energía. La energía nunca baja de 0, y con 0 el animal muere
    //Es synchronized porque varios atacantes, de hilos distintos, pueden herir al mismo animal a la vez.
    public synchronized boolean recepcionDanio(int cantidad) {
        boolean estabaVivo = consultaVida();
        actualizacionEnergia(consultaEnergia() - cantidad);
        if (estabaVivo && !consultaVida()) {
            muerte();
            return true; //solo si ESTE golpe fue el que lo mató, para que la eliminación se cuente una sola vez
        }
        return false;
    }

    //Lo que le pasa a un animal al morir: deja de moverse y de atacar.
    public synchronized void muerte() {
        limpiezaRuta();
        objetivoAtaque = null;
    }

    //Revisa si el animal puede atacar ahora y, si puede, anota este instante como su último ataque. 
    //Revisa y anota es una sola operación para que dos hilos
    //no lo hagan atacar dos veces en el mismo instante
    public synchronized boolean intentoAtaque(long ahora, int enfriamiento) {
        if (ahora - ultimoAtaque < enfriamiento) {
            return false;
        }
        ultimoAtaque = ahora;
        return true;
    }

    public synchronized void ordenAtaque(Animal objetivo) {
        this.objetivoAtaque = objetivo;
    }

    public synchronized void detencionAtaque() {
        this.objetivoAtaque = null;
    }

    public synchronized Animal consultaObjetivoAtaque() {
        return objetivoAtaque;
    }
}
