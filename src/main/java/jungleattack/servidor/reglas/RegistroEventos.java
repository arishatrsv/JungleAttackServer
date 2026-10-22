package jungleattack.servidor.reglas;

import java.util.ArrayList;
import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.modelo.EventoCombate;

//Lista de los eventos de combate recientes de una partida
//Los hilos de las zonas agregan eventos mientras el hilo que envía las actualizaciones los lee, 
//por eso todos los métodos son synchronized.
public class RegistroEventos {
    private final List<EventoCombate> eventos = new ArrayList<>();
    // Número de secuencia del primer evento de la lista (cambia cuando se descartan los más viejos).
    private long primeraSecuencia = 1;

    //Guarda un evento nuevo y devuelve su número de secuencia.
    public synchronized long registro(EventoCombate evento) {
        eventos.add(evento);
        if (eventos.size() > ConstantesJuego.MAXIMO_EVENTOS_GUARDADOS) {
            eventos.remove(0);
            primeraSecuencia++;
        }
        return primeraSecuencia + eventos.size() - 1;
    }

    //Número de secuencia del último evento registrado (0 si todavía no hay ninguno).
    public synchronized long ultimaSecuencia() {
        return primeraSecuencia + eventos.size() - 1;
    }

    //Los eventos del más viejo al más nuevo
    public synchronized List<EventoCombate> desde(long secuencia) {
        int inicio = (int) Math.max(0, secuencia + 1 - primeraSecuencia);
        if (inicio >= eventos.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(eventos.subList(inicio, eventos.size()));
    }
    
}
