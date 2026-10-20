package jungleattack.servidor.reglas;

import common.constantes.ConstantesComunes;
import java.util.ArrayList;
import java.util.List;
import jungleattack.servidor.mapa.BuscadorRutas;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.partidas.Jugador;


//Calcula las rutas y mueve los animales a velocidad constante
public class ReglasMovimiento {
    private final Mapa mapa;
    private final BuscadorRutas buscadorRutas;

    public ReglasMovimiento(Mapa mapa) {
        this.mapa = mapa;
        this.buscadorRutas = new BuscadorRutas(mapa);
    }

    // Ruta por los caminos entre dos puntos (ver BuscadorRutas).
    public List<Punto> calculoRuta(common.model.Punto origen, common.model.Punto destino) {
        return buscadorRutas.busquedaRuta(origen, destino);
    }

    //Revisa que la orden sea válida: el destino debe estar dentro del mapa, y debe haber al menos un animal
    //y todos deben estar vivos y ser de la manada del jugador
    public boolean validarMovimiento(Jugador jugador, List<Animal> animales, common.model.Punto destino) {
        if (jugador == null || jugador.consultaManada() == null || animales.isEmpty()) {
            return false;
        }
        if (destino.consultaPosicionX() < 0 || destino.consultaPosicionX() > mapa.consultaAncho()
                || destino.consultaPosicionY() < 0 || destino.consultaPosicionY() > mapa.consultaAlto()) {
            return false;
        }
        Manada manada = jugador.consultaManada();
        for (Animal animal : animales) {
            if (animal.consultaManada() != manada || !animal.consultaVida()) {
                return false;
            }
        }
        return true;
    }

    //Le da al animal la ruta hasta el destino.  Si se mueven varios animales a la vez, cada uno termina en un
    //lugar distinto
    public void inicioMovimiento(Animal animal, common.model.Punto destino, long ahora, int indice, int total) {
        synchronized (animal) {
            List<Punto> ruta = new ArrayList<>(calculoRuta(animal.consultaPosicion(), destino));
            if (total > 1 && !ruta.isEmpty()) {
                Punto llegada = Manada.posicionEnFormacion(ruta.get(ruta.size() - 1), indice, total);
                if (llegada.consultaPosicionX() != ruta.get(ruta.size() - 1).consultaPosicionX()
                        || llegada.consultaPosicionY() != ruta.get(ruta.size() - 1).consultaPosicionY()) {
                    ruta.add(llegada);
                }
            }
            animal.asignacionRuta(ruta, ahora);
        }
    }

    //Avanza al animal por su ruta según el tiempo que pasó desde la última vez: distancia = velocidad x tiempo.
    //Así la velocidad es constante aunque un ciclo se retrase.
    public boolean actualizacionPosicion(Animal animal, long ahora) {
        synchronized (animal) {
            if (!animal.consultaEnMovimiento()) {
                return false;
            }
            if (!animal.consultaVida()) {
                finalizacionMovimiento(animal);
                return false;
            }
            long transcurrido = ahora - animal.consultaUltimaActualizacion();
            if (transcurrido <= 0) {
                return false; // otro hilo ya lo actualizó en este instante
            }
            animal.actualizacionUltimaActualizacion(ahora);

            double restante = ConstantesComunes.ANIMAL_SPEED * transcurrido / 1000.0;
            double x = animal.consultaPosicionPrecisaX();
            double y = animal.consultaPosicionPrecisaY();
            List<Punto> ruta = animal.consultaRuta();
            int indice = animal.consultaIndiceRuta();
            while (restante > 0 && indice < ruta.size()) {
                double destinoX = ruta.get(indice).consultaPosicionX();
                double destinoY = ruta.get(indice).consultaPosicionY();
                double distancia = Math.hypot(destinoX - x, destinoY - y);
                if (distancia <= restante) {
                    x = destinoX; // llegó a este punto de la ruta y sigue hacia el próximo con lo que le sobra
                    y = destinoY;
                    restante -= distancia;
                    indice++;
                } else {
                    x += (destinoX - x) / distancia * restante;
                    y += (destinoY - y) / distancia * restante;
                    restante = 0;
                }
            }
            animal.actualizacionIndiceRuta(indice);
            animal.actualizacionPosicionPrecisa(x, y);
            if (indice >= ruta.size()) {
                finalizacionMovimiento(animal);
            }
            return true;
        }
    }
    // El animal llegó: borra su ruta.
    public void finalizacionMovimiento(Animal animal) {
        synchronized (animal) {
            animal.limpiezaRuta();
        }
    }
}
