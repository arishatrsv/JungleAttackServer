package jungleattack.servidor.reglas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.LongSupplier;
import common.constantes.ConstantesProtocolo;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.EventoCombate;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.Mensaje;

//Coordina una partida en curso mueve los animales y avisa a los clientes de los cambios.
//Crea un hilo por zona (HiloZona) que mueve a los animales, y un hilo aparte que cada cierto tiempo
//envía a cada jugador su STATE_UPDATE, pero solo si lo que ve cambió desde el último envío.
public class MotorJuego {
    private static final Logger REGISTRO = Logger.getLogger(MotorJuego.class.getName());
    private final SesionPartida sesion;
    private final ReglasMovimiento reglasMovimiento;
    private final ReglasObjetos reglasObjetos = new ReglasObjetos();
    private final RegistroEventos registroEventos = new RegistroEventos();
    private final ReglasCombate reglasCombate;
    private final ActualizadorPartida actualizador;
    private final LongSupplier reloj;
    private final List<HiloZona> hilosZona = new ArrayList<>();
    private final List<Thread> hilos = new ArrayList<>();
    private ScheduledExecutorService difusion;
    // Último contenido de STATE_UPDATE enviado a cada jugador. Sirve para no repetir mensajes iguales.
    private final Map<Jugador, String> ultimoEstadoEnviado = new ConcurrentHashMap<>();
    //Hasta qué evento de combate (número de secuencia) ya se revisó para cada jugador.
    private final Map<Jugador, Long> ultimoEventoVisto = new ConcurrentHashMap<>();
    private volatile boolean activo;

    //Debe crearse después de que la sesión tenga su mapa(cuando la partida inicia)
    public MotorJuego(SesionPartida sesion, LongSupplier reloj) {
        this.sesion = sesion;
        this.reloj = reloj;
        this.reglasMovimiento = new ReglasMovimiento(sesion.consultaMapa());
        this.reglasCombate = new ReglasCombate(sesion.consultaMapa(), sesion.consultaManadas(),
                registroEventos, reglasObjetos);
        this.actualizador = new ActualizadorPartida(sesion);
        for (Zona zona : sesion.consultaMapa().consultaZonas()) {
            hilosZona.add(new HiloZona(zona, sesion.consultaMapa(), reglasMovimiento, reloj, this::alMoverseUnAnimal,
                    reglasCombate::actualizacionAtaques));
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
            ultimoEventoVisto.put(jugador, registroEventos.ultimaSecuencia());
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

    // Revisa si el animal ganó al tocar una bandera o si recogió una taza.
    private void alMoverseUnAnimal(Animal animal) {
        if (comprobacionVictoria(animal)) {
            return;
        }
        reglasObjetos.recogidaTazas(animal, sesion.consultaTazas(), sesion.consultaMapa());
    }

    public boolean consultaActivo() {
        return activo;
    }

    //Si algo falla al enviar, la tarea programada no debe morir: se ignora ese envío y se intenta en el próximo.
    private void envioProtegido() {
        try {
            actualizacionClientes();
        } catch (RuntimeException e) {
            // Se reintenta en el siguiente ciclo, pero el fallo queda registrado.
            REGISTRO.log(Level.WARNING, "Falló el envío del estado a los clientes", e);
        }
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

    //Ejecuta una orden de movimiento. Cada animal calcula su ruta hasta el destino.
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

    //Ejecuta una orden de ataque, los animales dados atacan al objetivo mientras este siga vivo y al alcance.
    public int ejecucionOrdenAtaque(Jugador jugador, List<Animal> animales, Animal objetivo) {
        if (jugador.consultaManada() == null || objetivo.consultaManada() == jugador.consultaManada()
                || !objetivo.consultaVida() || !objetivoVisible(jugador, objetivo)) {
            return -1;
        }
        int atacando = 0;
        for (Animal atacante : animales) {
            if (atacante.consultaManada() == jugador.consultaManada() && atacante.consultaVida()
                    && reglasCombate.comprobacionAlcance(atacante, objetivo)) {
                atacante.ordenAtaque(objetivo);
                atacando++;
            }
        }
        return atacando;
    }

    // El jugador solo puede atacar animales que estén en alguna de las zonas que ve.
    private boolean objetivoVisible(Jugador jugador, Animal objetivo) {
        Zona zonaObjetivo = sesion.consultaMapa().zonaPorPosicion(objetivo.consultaPosicion());
        return zonaObjetivo != null && actualizador.zonasVisibles(jugador).contains(zonaObjetivo);
    }

    public boolean comprobacionVictoria(Animal animal) {
        Jugador ganador = sesion.consultaReglasVictoria().comprobacionVictoria(animal);
        return ganador != null && sesion.finalizacion(ganador, ConstantesProtocolo.RAZON_BANDERA);
    }

    //Envía a cada jugador su STATE_UPDATE, solo si lo que ve cambió desde el último envío
    public void actualizacionClientes() {
        for (Jugador jugador : sesion.consultaJugadores()) {
            // La foto sin eventos sirve para saber si algo cambió; los eventos nuevos se envían aunque nada más cambie.
            String contenido = actualizador.mensajeEstado(jugador).consultaContenido();
            List<EventoCombate> eventos = eventosParaElJugador(jugador);
            String anterior = ultimoEstadoEnviado.put(jugador, contenido);
            if (!contenido.equals(anterior) || !eventos.isEmpty()) {
                envioEstado(jugador, actualizador.mensajeEstado(jugador, eventos));
            }
        }
    }

    //Los eventos de combate que ocurrieron desde la última vez y que el jugador debe ver, las zonas
    //que ve ahora. Cada evento se le entrega una sola vez.
    private List<EventoCombate> eventosParaElJugador(Jugador jugador) {
        long desde = ultimoEventoVisto.getOrDefault(jugador, 0L);
        long hasta = registroEventos.ultimaSecuencia();
        List<EventoCombate> relevantes = new ArrayList<>();
        if (hasta <= desde) {
            return relevantes;
        }
        List<Zona> visibles = actualizador.zonasVisibles(jugador);
        for (EventoCombate evento : registroEventos.desde(desde)) {
            for (Zona zona : visibles) {
                if (zona.consultaIdentificador() == evento.consultaZonaAtacante()
                        || zona.consultaIdentificador() == evento.consultaZonaObjetivo()) {
                    relevantes.add(evento);
                    break;
                }
            }
        }
        ultimoEventoVisto.put(jugador, hasta);
        return relevantes;
    }

    // Entrega el mensaje al jugador. Es un método aparte para que las pruebas puedan registrar qué se envía.
    protected void envioEstado(Jugador jugador, Mensaje estado) {
        jugador.envioMensaje(estado);
    }

    public ReglasCombate consultaReglasCombate() {
        return reglasCombate;
    }

    public RegistroEventos consultaRegistroEventos() {
        return registroEventos;
    }

    public ReglasMovimiento consultaReglasMovimiento() {
        return reglasMovimiento;
    }
}

