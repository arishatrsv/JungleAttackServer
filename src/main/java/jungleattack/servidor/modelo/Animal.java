package jungleattack.servidor.modelo;


import common.model.Especie;
import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;

//Animal individual dentro de una partida. Hereda (identificador,energia,vida,posicion y especie)
public class Animal extends common.model.Animal {

    private List<Zona> rutaActual;
    private Punto destino;
    private boolean enMovimiento;
    private Animal objetivoAtaque;

    public Animal(String identificador, Especie especie, int energia, Punto posicion) {
        super(identificador, especie, energia, posicion);
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

    public void movimiento(Punto destino) {
        // TODO: implementar
    }

    public void actualizacionPosicion() {
        // TODO: implementar
    }

    public void recepcionDanio(int cantidad) {
        // TODO: implementar
    }

    public void muerte() {
        // TODO: implementar
    }

    public void ordenAtaque(Animal objetivo) {
        // TODO: implementar
    }

    public void detencionAtaque() {
        // TODO: implementar
    }
}
