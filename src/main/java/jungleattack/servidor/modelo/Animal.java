package jungleattack.servidor.modelo;

import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;

public class Animal extends common.model.Animal {

    private List<Zona> rutaActual;

    private Punto destino;

    private boolean enMovimiento;

    private Animal objetivoAtaque;

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
