package jungleattack.servidor.reglas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;

//Coordina una partida en curso mueve los animales y avisa a los clientes de los cambios.
//Crea un hilo por zona (HiloZona) que mueve a los animales, y un hilo aparte que cada cierto tiempo
//envía a cada jugador su STATE_UPDATE, pero solo si lo que ve cambió desde el último envío.
public class MotorJuego {
    private final SesionPartida sesion;
    private final ReglasMovimiento reglasMovimiento;
    private final ActualizadorPartida actualizador;
    private final LongSupplier reloj;
    private final List<HiloZona> hilosZona = new ArrayList<>();
    private final List<Thread> hilos = new ArrayList<>();
    private ScheduledExecutorService difusion;
    // Último contenido de STATE_UPDATE enviado a cada jugador. Sirve para no repetir mensajes iguales.
    private final Map<Jugador, String> ultimoEstadoEnviado = new ConcurrentHashMap<>();
    private volatile boolean activo;

    //Debe crearse después de que la sesión tenga su mapa(cuando la partida inicia)
    public MotorJuego(SesionPartida sesion, LongSupplier reloj) {
        this.sesion = sesion;
        this.reloj = reloj;
        this.reglasMovimiento = new ReglasMovimiento(sesion.consultaMapa());
        this.actualizador = new ActualizadorPartida(sesion);
        for (Zona zona : sesion.consultaMapa().consultaZonas()) {
            hilosZona.add(new HiloZona(zona, sesion.consultaMapa(), reglasMovimiento, reloj));
        }
    }
    //Arranca los hilos: uno por zona y el que envía las actualizaciones. Todos son daemon, así que no
    //impiden que el servidor termine.
    public synchronized void arranque() {
        if (activo) {
            return;
        }
        activo = true;
        // Lo que ya vieron los jugadores en GAME_STARTED no hace falta volver a enviarlo.
        for (Jugador jugador : sesion.consultaJugadores()) {
            ultimoEstadoEnviado.put(jugador, actualizador.mensajeEstado(jugador).consultaContenido());
        }
        for (HiloZona hiloZona : hilosZona) {
            Thread hilo = new Thread(hiloZona, ConstantesJuego.PREFIJO_HILO_ZONA + sesion.consultaCodigo());
            hilo.setDaemon(true);
            hilo.start();
            hilos.add(hilo);
        }
        difusion = Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, ConstantesJuego.NOMBRE_HILO_DIFUSION + sesion.consultaCodigo());
            hilo.setDaemon(true);
            return hilo;
        });
        difusion.scheduleAtFixedRate(this::envioProtegido, ConstantesJuego.INTERVALO_DIFUSION_MS,
                ConstantesJuego.INTERVALO_DIFUSION_MS, TimeUnit.MILLISECONDS);
    }

    // Detiene todos los hilos. Se llama cuando la partida termina o se cancela.
    public synchronized void cierre() {
        activo = false;
        for (HiloZona hiloZona : hilosZona) {
            hiloZona.cierre();
        }
        for (Thread hilo : hilos) {
            hilo.interrupt();
        }
        hilos.clear();
        if (difusion != null) {
            difusion.shutdownNow();
            difusion = null;
        }
    }

    public boolean consultaActivo() {
        return activo;
    }

    public void actualizacionJuego() {
        // TODO: implementar
    }

    // Si algo falla al enviar, la tarea programada no debe morir: se ignora ese envío y se intenta en el próximo.
    private void envioProtegido() {
        actualizacionClientes();
    }

    //Ciclo completo sin hilos mueve todos los animales hasta el instante actual del reloj y envía
    //las actualizaciones
    public void actualizacionJuego() {
        long ahora = reloj.getAsLong();
        for (HiloZona hiloZona : hilosZona) {
            hiloZona.actualizacionZona(ahora);
        }
        actualizacionClientes();
    }

    public boolean ejecucionOrdenMovimiento(Jugador jugador, List<Animal> animales,jungleattack.servidor.mapa.Punto destino) {
        if (!reglasMovimiento.validarMovimiento(jugador, animales, destino)) {
            return false;
        }
        long ahora = reloj.getAsLong();
        for (int i = 0; i < animales.size(); i++) {
            reglasMovimiento.inicioMovimiento(animales.get(i), destino, ahora, i, animales.size());
        }
        return true;
    }

    public void ejecucionOrdenAtaque(Jugador jugador, List<Animal> animales, Animal objetivo) {
        // TODO: implementar
    }

    public boolean comprobacionVictoria() {
        // TODO: implementar
        return false;
    }
    //Envía a cada jugador su STATE_UPDATE, solo si lo que ve cambió desde el último envío
    public void actualizacionClientes() {
        for (Jugador jugador : sesion.consultaJugadores()) {
            Mensaje estado = actualizador.mensajeEstado(jugador);
            String anterior = ultimoEstadoEnviado.put(jugador, estado.consultaContenido());
            if (!estado.consultaContenido().equals(anterior)) {
                envioEstado(jugador, estado);
            }
        }
    }
    // Entrega el mensaje al jugador. Es un método aparte para que las pruebas puedan registrar qué se envía.
    protected void envioEstado(Jugador jugador, Mensaje estado) {
        jugador.envioMensaje(estado);
    }

    public ReglasMovimiento consultaReglasMovimiento() {
        return reglasMovimiento;
    }
}
