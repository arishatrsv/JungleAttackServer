package jungleattack.servidor.partidas;

import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.concurrent.CopyOnWriteArrayList;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.ManadaBots;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.red.ManejadorCliente;
import jungleattack.servidor.reglas.ActualizadorPartida;
import jungleattack.servidor.reglas.MotorJuego;
import jungleattack.servidor.reglas.ReglasObjetos;
import jungleattack.servidor.reglas.ReglasVictoria;

// Representa una partida en curso, con sus jugadores, estado y elementos del juego.
public class SesionPartida {
    // Código con el que el otro jugador se une a la partida.
    private final String codigo;
    // Lista de jugadores que participan en la partida. Se agregan a medida que se unen.
    private final List<Jugador> jugadores = new CopyOnWriteArrayList<>();
    // volatile porque lo cambian y lo leen hilos distintos.
    private volatile EstadoPartida estado;
    // Se crea al iniciar la partida. volatile porque lo leen otros hilos.
    private volatile Mapa mapa;
    // Las 7 manadas: las 2 de los jugadores y las 5 de los bots.
    private final List<Manada> manadas = new CopyOnWriteArrayList<>();
    private final List<Taza> tazas = new CopyOnWriteArrayList<>();

    // Una bandera por jugador gana quien toque la del oponente.
    private final List<Bandera> banderas = new CopyOnWriteArrayList<>();
    private ResumenPartida resumen;
    //Instante (milisegundos) en que empezó la partida.
    private long inicioPartida;
    private volatile ReglasVictoria reglasVictoria;
    //Qué hacer cuando la partida termina (el gestor la saca de sus partidas activas).
    private volatile Runnable alFinalizar = () -> { };
    //Coordina el movimiento y las actualizaciones. Se crea al iniciar la partida.
    private volatile MotorJuego motorJuego;
    //Si es false el motor no arranca sus hilos .
    private final boolean motorActivo;
    private final LongSupplier reloj;
    private final Random aleatorio; //para poder repetir el sorteo de zonas y tazas en las pruebas.

    // Crea la partida en estado de espera, hasta que llegue el segundo jugador.
    public SesionPartida(String codigo) {
        this(codigo, new Random());
    }

    // Constructor con generador controlado para que las pruebas sean repetibles.
    public SesionPartida(String codigo, Random aleatorio) {
        this(codigo, aleatorio, true);
    }

    public SesionPartida(String codigo, Random aleatorio, boolean motorActivo) {
        this(codigo, aleatorio, motorActivo, System::currentTimeMillis);
    }

    public SesionPartida(String codigo, Random aleatorio, boolean motorActivo, LongSupplier reloj) {
        this.codigo = codigo;
        this.aleatorio = aleatorio;
        this.motorActivo = motorActivo; //el motor debe arrancar los hilos al empezar la partida
        this.reloj = reloj; //fuente del tiempo
        this.estado = EstadoPartida.ESPERANDO_JUGADOR;
    }

    //Agrega un jugador a la partida.
    //Synchronized para que no se agreguen dos jugadores a la vez y se pase de 2.
    public synchronized boolean registroJugador(Jugador jugador) {
        if (estado != EstadoPartida.ESPERANDO_JUGADOR
                || jugadores.size() >= ConstantesJuego.CANTIDAD_JUGADORES) {
            return false; //La partida ya está llena o ya no esta en espera de jugadores.
        }
        jugadores.add(jugador);
        if (jugadores.size() == ConstantesJuego.CANTIDAD_JUGADORES) {
            estado = EstadoPartida.SELECCION_ESPECIE; //Ya hay dos jugadores, se pasa a la fase de selección de especie.
        }
        return true;
    }

    //Guarda la especie que escoge un jugador.
    //Si los dos jugadores eligen el mismo animal, solo el primero se lo queda.
    public synchronized boolean eleccionEspecie(Jugador jugador, Especie especie) {
        if (estado != EstadoPartida.SELECCION_ESPECIE || !especiesDisponibles().contains(especie)) {
            return false; //Ya la especie la tiene otro jugador
        }
        jugador.seleccionEspecie(especie); //Guarda la especie elegida por el jugador
        return true;
    }

    // Especies que todavía nadie ha elegido
    public synchronized List<Especie> especiesDisponibles() {
        List<Especie> disponibles = new ArrayList<>(List.of(Especie.values()));
        for (Jugador jugador : jugadores) {
            disponibles.remove(jugador.consultaEspecie()); // si es null no quita nada
        }
        return disponibles;
    }

    // Dice si los dos jugadores ya eligieron su especie.
    public synchronized boolean seleccionCompleta() {
        if (jugadores.size() < ConstantesJuego.CANTIDAD_JUGADORES) { //Todavía no hay dos jugadores
            return false;
        }
        for (Jugador jugador : jugadores) { 
            if (jugador.consultaEspecie() == null) { //todavía no eligió
                return false;
            }
        }
        return true; // los dos eligieron su especie
    }

    //Cancela la partida y avisa a los jugadores conectados.
    public synchronized void cancelacion(String motivo) {
        estado = EstadoPartida.FINALIZADA;
        if (motorJuego != null) {
            motorJuego.cierre(); // se detienen los hilos de la partida
        }
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConectado()) {
                // Primero lo saca de la partida y DESPUÉS le avisa. 
                jugador.salidaDePartida();
                jugador.envioMensaje(ConversorMensajes.creacionMensaje(
                        ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
            }
        }
    }

    //Cuenta los jugadores que siguen conectados.
    public int cantidadJugadoresConectados() {
        int conectados = 0;
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConectado()) {
                conectados++;
            }
        }
        return conectados;
    }

    //Envia a cada jugador el estado de la partida, con las especies elegidas y las disponibles.
    public void actualizacionEstado() {
        if (estado != EstadoPartida.SELECCION_ESPECIE) {
            return; //Solo envia el estado de selección de especie
        }
        String disponibles = unionIdentificadores(especiesDisponibles());
        String completa = Boolean.toString(seleccionCompleta());
        for (Jugador jugador : jugadores) {
            Jugador oponente = oponenteDe(jugador);
            jugador.envioMensaje(ConversorMensajes.creacionMensaje( //Envía a cada jugador el estado de la partida
                    ConstantesProtocolo.SELECCION_ANIMALES,
                    ConstantesProtocolo.CAMPO_DISPONIBLES, disponibles,
                    ConstantesProtocolo.CAMPO_PROPIA, identificadorDe(jugador.consultaEspecie()),
                    ConstantesProtocolo.CAMPO_OPONENTE, identificadorDe(oponente == null ? null : oponente.consultaEspecie()),
                    ConstantesProtocolo.CAMPO_COMPLETA, completa));
        }
    }

    //Busca el jugador que corresponde a una conexión, o null si no está en esta partida.
    public Jugador jugadorPorConexion(ManejadorCliente conexion) {
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConexion() == conexion) {
                return jugador;
            }
        }
        return null;
    }

    public String consultaCodigo() {
        return codigo;
    }

    public EstadoPartida consultaEstado() {
        return estado;
    }

    public List<Jugador> consultaJugadores() {
        return List.copyOf(jugadores); // copia de solo lectura
    }

    //Empieza la partida cuando los dos jugadores ya eligieron su especie
    public synchronized boolean inicio() {
        if (estado != EstadoPartida.SELECCION_ESPECIE || !seleccionCompleta()) { // Solo inicia si sigue en selección y todos los jugadores eligieron especie.
            return false; // Si no se cumple alguna condición, no modifica la partida.
        }
        Mapa nuevoMapa = new Mapa(); // Crea el mapa que se usará durante esta partida.
        nuevoMapa.generacionMapa(); // Genera sus zonas y demás elementos del terreno.

        // Se mezclan las zonas y se reparten en orden: así las 7 manadas quedan en zonas distintas.
        List<Zona> zonasSorteadas = new ArrayList<>(nuevoMapa.consultaZonas()); // Copia las zonas para poder mezclarlas sin alterar la lista del mapa.
        Collections.shuffle(zonasSorteadas, aleatorio); // Sortea el orden de reparto usando el generador aleatorio de la partida.
        int siguienteZona = 0; // Lleva el índice de la próxima zona que se asignará.

        for (Jugador jugador : jugadores) {
            Zona zona = zonasSorteadas.get(siguienteZona++); // Reserva para este jugador una zona distinta de las ya repartidas.
            ManadaJugador manada = new ManadaJugador(jugador.consultaEspecie()); // Crea su manada con la especie que seleccionó.
            ubicacionManada(manada, ConstantesJuego.CANTIDAD_ANIMALES_JUGADOR, zona); // Crea y registra sus animales en la zona y en la partida.
            jugador.asignacionManada(manada); // Vincula la manada creada con el jugador.
            banderas.add(new Bandera(jugador, zona)); // Coloca la bandera del jugador en su zona inicial.
        }
        // Los bots son las especies que los jugadores no eligieron (cada una en una zona distinta).
        for (Especie especie : especiesDisponibles()) {
            Zona zona = zonasSorteadas.get(siguienteZona++); // Asigna a cada especie de bot una zona todavía libre.
            ubicacionManada(new ManadaBots(especie), ConstantesJuego.CANTIDAD_ANIMALES_BOTS, zona); // Crea sus animales y los registra en esa zona.
        }

        for (Taza taza : new ReglasObjetos().colocacionTazas(nuevoMapa, aleatorio)) {
            tazas.add(taza); // Guarda la taza en la lista de objetos de esta partida.
            nuevoMapa.zonaPorPosicion(taza.consultaPosicion()).registroTaza(taza); // La registra también en la zona del mapa donde quedó colocada.
        }

        inicioPartida = reloj.getAsLong();
        reglasVictoria = new ReglasVictoria(consultaBanderas(), consultaJugadores());
        mapa = nuevoMapa;
        // El motor se crea antes de pasar a EN_CURSO: así un comando que llegue apenas empieza la partida ya lo encuentra.
        motorJuego = new MotorJuego(this, reloj);
        estado = EstadoPartida.EN_CURSO;
        new ActualizadorPartida(this).envioInicio();
        if (motorActivo) {
            motorJuego.arranque();
        }
        return true;
    }

    // Crea los animales de una manada alrededor del centro de la zona y los registra en ella.
    private void ubicacionManada(Manada manada, int cantidad, Zona zona) {
        manada.creacionAnimales(cantidad, zona.consultaPosicion());
        for (Animal animal : manada.consultaAnimales()) {
            zona.registroAnimal(animal);
        }
        manadas.add(manada);
    }

    //Termina la partida, gana un jugador.Guarda el resumen detiene el motor y le envía GAME_OVER a cada jugador
    //Es synchronized y revisa el estado, así que si dos animales tocan las banderas
    //casi al mismo tiempo (desde hilos distintos), gana solo el primero
    public synchronized boolean finalizacion(Jugador ganador, String razon) {
        if (estado != EstadoPartida.EN_CURSO) {
            return false;
        }
        estado = EstadoPartida.FINALIZADA;
        resumen = reglasVictoria.creacionResumen(ganador, razon, inicioPartida, reloj.getAsLong());
        if (motorJuego != null) {
            motorJuego.cierre();
        }
        ActualizadorPartida actualizador = new ActualizadorPartida(this);
        for (Jugador jugador : jugadores) {
            if (jugador.consultaConectado()) {
                jugador.salidaDePartida();
                jugador.envioMensaje(actualizador.mensajeFin(jugador));
            }
        }
        alFinalizar.run();
        return true;
    }

    // Define qué hacer cuando la partida termine. Lo usa el gestor para sacar la partida de las activas.
    public void registroAlFinalizar(Runnable accion) {
        this.alFinalizar = accion;
    }

    public ReglasVictoria consultaReglasVictoria() {
        return reglasVictoria;
    }

    // Busca un animal de la partida (de cualquier manada) por su identificador, o null si no existe.
    public Animal animalPorIdentificador(String identificador) {
        for (Manada manada : manadas) {
            Animal animal = manada.animalPorIdentificador(identificador);
            if (animal != null) {
                return animal;
            }
        }
        return null;
    }

    public List<Animal> animalesEnZona(Zona zona) {
        return zona.animalesEnZona();
    }

    public Manada manadaPorJugador(Jugador jugador) {
        return jugador.consultaManada();
    }

    // Mapa de la partida: es null hasta que la partida empieza.
    public Mapa consultaMapa() {
        return mapa;
    }

    public List<Manada> consultaManadas() {
        return List.copyOf(manadas);
    }

    public List<Taza> consultaTazas() {
        return List.copyOf(tazas);
    }

    public List<Bandera> consultaBanderas() {
        return List.copyOf(banderas);
    }

    public MotorJuego consultaMotor() {
        return motorJuego;
    }

    public ResumenPartida consultaResumen() {
        return resumen;
    }

    // Devuelve el otro jugador de la partida, o null si todavía no hay otro.
    private Jugador oponenteDe(Jugador jugador) {
        for (Jugador otro : jugadores) { //Busca al otro jugador
            if (otro != jugador) { //Si es distinto del jugador que se pasa como parámetro, es el oponente
                return otro;
            }
        }
        return null;
    }

    // Identificador de una especie
    private String identificadorDe(Especie especie) {
        return especie == null ? "" : especie.consultaIdentificador(); 
    }

    //Une los identificadores de una lista de especies en un solo texto separado por comas.
    private String unionIdentificadores(List<Especie> especies) {
        List<String> identificadores = new ArrayList<>();
        for (Especie especie : especies) {
            identificadores.add(especie.consultaIdentificador());
        }
        return String.join(ConstantesProtocolo.SEPARADOR_LISTA, identificadores);
    }

}
