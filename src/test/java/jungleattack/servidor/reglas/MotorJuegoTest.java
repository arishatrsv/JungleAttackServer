package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;
import org.junit.jupiter.api.Test;

class MotorJuegoTest {

    private final AtomicLong reloj = new AtomicLong(0);

    // Partida ya iniciada, sin hilos y con un reloj que las pruebas avanzan a mano.
    private SesionPartida partida() {
        SesionPartida sesion = new SesionPartida("AB12", new Random(4), false, reloj::get);
        Jugador ana = new Jugador("Ana", null);
        Jugador luis = new Jugador("Luis", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(luis);
        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        sesion.eleccionEspecie(luis, Especie.TUCAN);
        sesion.inicio();
        return sesion;
    }

    // Avanza el reloj y el juego, ciclo por ciclo, hasta que pase ese tiempo.
    private void avanzar(SesionPartida sesion, long milisegundos) {
        long fin = reloj.get() + milisegundos;
        while (reloj.get() < fin) {
            reloj.addAndGet(50);
            sesion.consultaMotor().actualizacionJuego();
        }
    }

    private Zona zonaInicial(SesionPartida sesion, Jugador jugador) {
        return new ActualizadorPartida(sesion).zonasVisibles(jugador).get(0);
    }

    private Punto centro(Zona zona) {
        return new Punto(zona.consultaPosicion().consultaPosicionX(), zona.consultaPosicion().consultaPosicionY());
    }

    @Test
    void unaOrdenValidaMueveALosAnimalesHastaElDestino() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Zona origen = zonaInicial(sesion, ana);
        Zona vecina = sesion.consultaMapa().zonasConectadas(origen).get(0);
        Animal animal = ana.consultaManada().consultaAnimales().get(0);

        assertTrue(sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(animal), centro(vecina)));
        avanzar(sesion, 10000);

        assertEquals(vecina.consultaPosicion().consultaPosicionX(), animal.consultaPosicion().consultaPosicionX());
        assertEquals(vecina.consultaPosicion().consultaPosicionY(), animal.consultaPosicion().consultaPosicionY());
        assertTrue(vecina.animalesEnZona().contains(animal));
        assertFalse(origen.animalesEnZona().contains(animal));
    }

    @Test
    void noSePuedeOrdenarMoverLosAnimalesDelOtroJugador() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        Animal ajeno = luis.consultaManada().consultaAnimales().get(0);
        Punto antes = new Punto(ajeno.consultaPosicion().consultaPosicionX(), ajeno.consultaPosicion().consultaPosicionY());

        assertFalse(sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(ajeno), new Punto(500, 300)));
        avanzar(sesion, 2000);

        assertEquals(antes.consultaPosicionX(), ajeno.consultaPosicion().consultaPosicionX());
        assertEquals(antes.consultaPosicionY(), ajeno.consultaPosicion().consultaPosicionY());
    }

    @Test
    void sinOrdenesLosAnimalesNoSeMueven() {
        SesionPartida sesion = partida();
        Animal animal = sesion.consultaJugadores().get(0).consultaManada().consultaAnimales().get(0);
        int x = animal.consultaPosicion().consultaPosicionX();
        avanzar(sesion, 5000);
        assertEquals(x, animal.consultaPosicion().consultaPosicionX());
    }

    @Test
    void moverTodaLaManadaACasaAjenaCambiaLaZonaQueSeVe() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Zona origen = zonaInicial(sesion, ana);
        Zona destino = sesion.consultaMapa().zonasConectadas(origen).get(0);
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, ana.consultaManada().consultaAnimales(), centro(destino));
        avanzar(sesion, 15000);

        List<Zona> visibles = actualizador.zonasVisibles(ana);
        assertEquals(1, visibles.size());
        assertEquals(destino.consultaIdentificador(), visibles.get(0).consultaIdentificador());
        assertEquals(10, visibles.get(0).animalesEnZona().stream().filter(a -> a.consultaManada() == ana.consultaManada()).count());
    }

    @Test
    void siSoloSaleUnAnimalElJugadorVeLasDosZonas() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Zona origen = zonaInicial(sesion, ana);
        Zona destino = sesion.consultaMapa().zonasConectadas(origen).get(0);
        Animal viajero = ana.consultaManada().consultaAnimales().get(0);
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(viajero), centro(destino));
        avanzar(sesion, 15000);

        List<Integer> ids = new ArrayList<>();
        for (Zona zona : actualizador.zonasVisibles(ana)) {
            ids.add(zona.consultaIdentificador());
        }
        assertTrue(ids.contains(origen.consultaIdentificador()));
        assertTrue(ids.contains(destino.consultaIdentificador()));
        JsonObject estado = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana));
        assertEquals(2, estado.getAsJsonArray(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES).size());
        assertEquals(10, estado.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).size());
    }

    // ----- Qué se envía a los clientes -----

    // Motor que guarda lo que enviaría, en lugar de mandarlo por un socket.
    private static class MotorRegistrador extends MotorJuego {
        private final List<String> tipos = new ArrayList<>();

        MotorRegistrador(SesionPartida sesion, java.util.function.LongSupplier reloj) {
            super(sesion, reloj);
        }

        @Override
        protected void envioEstado(Jugador jugador, Mensaje estado) {
            tipos.add(estado.consultaTipo());
        }
    }

    @Test
    void lasActualizacionesSoloSeEnvianCuandoAlgoCambio() {
        SesionPartida sesion = partida();
        MotorRegistrador motor = new MotorRegistrador(sesion, reloj::get);
        Jugador ana = sesion.consultaJugadores().get(0);

        motor.actualizacionClientes(); // primera vez: no tenían nada
        int primeros = motor.tipos.size();
        assertEquals(2, primeros);
        assertEquals(ConstantesProtocolo.ACTUALIZACION_ESTADO, motor.tipos.get(0));

        motor.actualizacionClientes(); // nada cambió: no se envía nada
        assertEquals(primeros, motor.tipos.size());

        // Un animal de Ana se mueve: solo cambia lo que ve Ana (el estado de Luis es el mismo).
        Animal animal = ana.consultaManada().consultaAnimales().get(0);
        animal.actualizacionPosicion(new Punto(animal.consultaPosicion().consultaPosicionX() + 5, animal.consultaPosicion().consultaPosicionY()));
        motor.actualizacionClientes();
        assertEquals(primeros + 1, motor.tipos.size());
    }

    @Test
    void elMotorNoEstaActivoHastaQueArranca() {
        SesionPartida sesion = partida();
        MotorJuego motor = sesion.consultaMotor();
        assertFalse(motor.consultaActivo());
        motor.arranque();
        assertTrue(motor.consultaActivo());
        motor.arranque(); // llamarlo otra vez no hace nada
        motor.cierre();
        assertFalse(motor.consultaActivo());
        motor.cierre(); // cerrar dos veces tampoco falla
    }

    // ----- Tazas -----

    private int tazasConsumidasEnLaPartida(SesionPartida sesion) {
        int consumidas = 0;
        for (Taza taza : sesion.consultaTazas()) {
            if (taza.consultaConsumida()) {
                consumidas++;
            }
        }
        return consumidas;
    }

    @Test
    void unAnimalQueLlegaAUnaTazaLaConsumeYSubeElDanioDeSuManada() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Taza objetivo = sesion.consultaTazas().get(0);
        Animal animal = ana.consultaManada().consultaAnimales().get(0);
        Zona zonaDeLaTaza = sesion.consultaMapa().zonaPorPosicion(objetivo.consultaPosicion());

        assertTrue(sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(animal), objetivo.consultaPosicion()));
        avanzar(sesion, 60000);

        assertTrue(objetivo.consultaConsumida(), "el animal debió consumir la taza al llegar");
        assertFalse(zonaDeLaTaza.tazasEnZona().contains(objetivo));
        // Pudo recoger otras tazas en el camino: el daño sube una vez por cada taza consumida.
        int consumidas = ana.consultaManada().consultaTazasConsumidas();
        assertTrue(consumidas >= 1);
        assertEquals(2 + consumidas, ana.consultaManada().consultaDanio());
    }

    @Test
    void elDanioSubeParaTodaLaManadaYNoParaLaDelOponente() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        Taza objetivo = sesion.consultaTazas().get(0);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(ana.consultaManada().consultaAnimales().get(0)), objetivo.consultaPosicion());
        avanzar(sesion, 60000);

        assertTrue(ana.consultaManada().consultaDanio() > 2);
        assertEquals(2, luis.consultaManada().consultaDanio());
        assertEquals(0, luis.consultaManada().consultaTazasConsumidas());
    }

    @Test
    void unaTazaConsumidaDejaDeVerseEnLasActualizaciones() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Taza objetivo = sesion.consultaTazas().get(0);
        Animal animal = ana.consultaManada().consultaAnimales().get(0);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(animal), objetivo.consultaPosicion());
        avanzar(sesion, 60000);

        JsonObject estado = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana));
        assertEquals(ana.consultaManada().consultaTazasConsumidas(), estado.get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
        assertEquals(ana.consultaManada().consultaDanio(), estado.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());
        estado.getAsJsonArray(ConstantesProtocolo.CAMPO_TAZAS).forEach(taza ->
                assertFalse(objetivo.consultaIdentificador().equals(
                        taza.getAsJsonObject().get(ConstantesProtocolo.CAMPO_ID).getAsString())));
    }

    @Test
    void variosAnimalesQueLlegaranJuntosConsumenLaTazaUnaSolaVez() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        Taza objetivo = sesion.consultaTazas().get(0);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, ana.consultaManada().consultaAnimales(), objetivo.consultaPosicion());
        avanzar(sesion, 90000);

        // Cada taza consumida cuenta una sola vez, entre todos los jugadores.
        assertEquals(tazasConsumidasEnLaPartida(sesion),
                ana.consultaManada().consultaTazasConsumidas() + luis.consultaManada().consultaTazasConsumidas());
        assertEquals(2 + ana.consultaManada().consultaTazasConsumidas(), ana.consultaManada().consultaDanio());
    }

    @Test
    void siNadieSeMueveNingunaTazaSeConsume() {
        SesionPartida sesion = partida();
        avanzar(sesion, 20000);
        assertEquals(0, tazasConsumidasEnLaPartida(sesion));
    }
}
