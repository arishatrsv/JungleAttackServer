package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalBot;
import jungleattack.servidor.modelo.ManadaBots;
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

    // ----- Combate -----

    // Motor que además guarda los mensajes que enviaría, para revisar qué recibe cada jugador.
    private static class MotorQueGuarda extends MotorJuego {
        private final List<Jugador> destinatarios = new ArrayList<>();
        private final List<Mensaje> mensajes = new ArrayList<>();

        MotorQueGuarda(SesionPartida sesion, java.util.function.LongSupplier reloj) {
            super(sesion, reloj);
        }

        @Override
        protected void envioEstado(Jugador jugador, Mensaje estado) {
            destinatarios.add(jugador);
            mensajes.add(estado);
        }

        List<Mensaje> mensajesDe(Jugador jugador) {
            List<Mensaje> suyos = new ArrayList<>();
            for (int i = 0; i < mensajes.size(); i++) {
                if (destinatarios.get(i) == jugador) {
                    suyos.add(mensajes.get(i));
                }
            }
            return suyos;
        }
    }

    private void avanzar(MotorJuego motor, long milisegundos) {
        long fin = reloj.get() + milisegundos;
        while (reloj.get() < fin) {
            reloj.addAndGet(50);
            motor.actualizacionJuego();
        }
    }

    // La primera manada de bots de la partida.
    private ManadaBots botsDe(SesionPartida sesion) {
        return (ManadaBots) sesion.consultaManadas().stream().filter(m -> m instanceof ManadaBots).findFirst().get();
    }

    // Pone al animal en un punto del mapa, como si hubiera llegado caminando hasta ahí.
    private void colocar(SesionPartida sesion, Animal animal, int x, int y) {
        animal.actualizacionPosicion(new Punto(x, y));
        sesion.consultaMapa().actualizacionUbicacionAnimal(animal);
    }

    private Punto centroDeLaZonaDe(SesionPartida sesion, Animal animal) {
        return centro(sesion.consultaMapa().zonaPorPosicion(animal.consultaPosicion()));
    }

    @Test
    void unBotAtacaSoloAlAnimalQueSeLeAcerca() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Animal visitante = ana.consultaManada().consultaAnimales().get(0);
        Punto centroBots = centroDeLaZonaDe(sesion, botsDe(sesion).consultaAnimales().get(0));

        avanzar(sesion, 2000);
        assertEquals(20, visitante.consultaEnergia()); // lejos de los bots no pasa nada

        colocar(sesion, visitante, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        avanzar(sesion, 50);
        assertEquals(16, visitante.consultaEnergia()); // los 4 bots atacan en cuanto lo tienen al alcance
        avanzar(sesion, 1000);
        assertEquals(12, visitante.consultaEnergia()); // y siguen una vez por segundo
    }

    @Test
    void siUnAnimalMuereDejaDeVerseYSeCuentaComoPerdido() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Animal debil = ana.consultaManada().consultaAnimales().get(0);
        debil.actualizacionEnergia(3);
        Punto centroBots = centroDeLaZonaDe(sesion, botsDe(sesion).consultaAnimales().get(0));

        colocar(sesion, debil, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        avanzar(sesion, 50);

        assertFalse(debil.consultaVida());
        assertEquals(1, ana.consultaManada().cantidadAnimalesMuertos());
        JsonArray animales = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana)).getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES);
        for (JsonElement animal : animales) {
            assertFalse(debil.consultaIdentificador().equals(animal.getAsJsonObject().get(ConstantesProtocolo.CAMPO_ID).getAsString()));
        }
    }

    @Test
    void unaOrdenDeAtaqueEliminaBotsYSeCuentan() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ManadaBots bots = botsDe(sesion);
        Punto centroBots = centroDeLaZonaDe(sesion, bots.consultaAnimales().get(0));
        for (Animal animal : ana.consultaManada().consultaAnimales()) {
            colocar(sesion, animal, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        }

        MotorJuego motor = sesion.consultaMotor();
        assertEquals(10, motor.ejecucionOrdenAtaque(ana, ana.consultaManada().consultaAnimales(), bots.consultaAnimales().get(0)));
        avanzar(sesion, 50); // 10 atacantes x 2 de daño: el bot (10 de energía) muere en el primer ciclo
        assertFalse(bots.consultaAnimales().get(0).consultaVida());
        assertEquals(1, ana.consultaManada().consultaBotsEliminados());

        assertEquals(10, motor.ejecucionOrdenAtaque(ana, ana.consultaManada().consultaAnimales(), bots.consultaAnimales().get(1)));
        avanzar(sesion, 1000);
        assertFalse(bots.consultaAnimales().get(1).consultaVida());
        assertEquals(2, ana.consultaManada().consultaBotsEliminados());
    }

    @Test
    void lasOrdenesDeAtaqueInvalidasSeRechazan() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        ManadaBots bots = botsDe(sesion);
        MotorJuego motor = sesion.consultaMotor();
        List<Animal> mios = ana.consultaManada().consultaAnimales();

        assertEquals(-1, motor.ejecucionOrdenAtaque(ana, mios, mios.get(1))); // un animal propio
        assertEquals(-1, motor.ejecucionOrdenAtaque(ana, mios, luis.consultaManada().consultaAnimales().get(0))); // zona que no se ve
        assertEquals(-1, motor.ejecucionOrdenAtaque(ana, mios, bots.consultaAnimales().get(0))); // zona que no se ve
        Animal muerto = bots.consultaAnimales().get(0);
        muerto.recepcionDanio(100);
        assertEquals(-1, motor.ejecucionOrdenAtaque(ana, mios, muerto)); // ya murió
    }

    @Test
    void losAtacantesFueraDeAlcanceIgnoranLaOrden() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ManadaBots bots = botsDe(sesion);
        Punto centroBots = centroDeLaZonaDe(sesion, bots.consultaAnimales().get(0));
        Animal cerca = ana.consultaManada().consultaAnimales().get(0);
        Animal lejos = ana.consultaManada().consultaAnimales().get(1);
        colocar(sesion, cerca, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        Animal objetivo = bots.consultaAnimales().get(0);
        // El otro animal se queda en la misma zona pero a más de 40 unidades del objetivo.
        int xLejos = objetivo.consultaPosicion().consultaPosicionX() + 90;
        colocar(sesion, lejos, xLejos, objetivo.consultaPosicion().consultaPosicionY());

        MotorJuego motor = sesion.consultaMotor();
        assertEquals(0, motor.ejecucionOrdenAtaque(ana, List.of(lejos), objetivo));
        assertEquals(1, motor.ejecucionOrdenAtaque(ana, List.of(cerca, lejos), objetivo));
    }

    @Test
    void moverAUnAnimalQueAtacabaDetieneElAtaque() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ManadaBots bots = botsDe(sesion);
        Punto centroBots = centroDeLaZonaDe(sesion, bots.consultaAnimales().get(0));
        Animal atacante = ana.consultaManada().consultaAnimales().get(0);
        colocar(sesion, atacante, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        sesion.consultaMotor().ejecucionOrdenAtaque(ana, List.of(atacante), bots.consultaAnimales().get(0));
        assertTrue(atacante.consultaObjetivoAtaque() != null);

        sesion.consultaMotor().ejecucionOrdenMovimiento(ana, List.of(atacante), new Punto(centroBots.consultaPosicionX() + 50, centroBots.consultaPosicionY()));
        assertEquals(null, atacante.consultaObjetivoAtaque());
    }

    @Test
    void losEventosDelCombateLlegaSoloAQuienVeLaZonaYUnaSolaVez() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        MotorQueGuarda motor = new MotorQueGuarda(sesion, reloj::get);
        Animal visitante = ana.consultaManada().consultaAnimales().get(0);
        Punto centroBots = centroDeLaZonaDe(sesion, botsDe(sesion).consultaAnimales().get(0));

        motor.actualizacionClientes(); // la primera vez los dos reciben su foto inicial: se parte de ahí
        int mensajesAna = motor.mensajesDe(ana).size();
        int mensajesLuis = motor.mensajesDe(luis).size();
        colocar(sesion, visitante, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());

        avanzar(motor, 50);

        List<Mensaje> deAna = motor.mensajesDe(ana);
        assertEquals(mensajesAna + 1, deAna.size());
        JsonArray eventos = ConversorMensajes.contenidoComoObjeto(deAna.get(deAna.size() - 1)).getAsJsonArray(ConstantesProtocolo.CAMPO_EVENTOS);
        assertEquals(4, eventos.size()); // los 4 bots atacaron
        for (JsonElement elemento : eventos) {
            JsonObject evento = elemento.getAsJsonObject();
            assertEquals(ConstantesProtocolo.EVENTO_ATAQUE, evento.get(ConstantesProtocolo.CAMPO_TIPO_EVENTO).getAsString());
            assertEquals(visitante.consultaIdentificador(), evento.get(ConstantesProtocolo.CAMPO_OBJETIVO).getAsString());
            assertEquals(1, evento.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());
        }
        assertEquals(mensajesLuis, motor.mensajesDe(luis).size()); // a Luis no le llegó nada nuevo: no ve esa zona

        // Sin nada nuevo no se repite lo ya enviado.
        int antes = motor.mensajesDe(ana).size();
        avanzar(motor, 500);
        assertEquals(antes, motor.mensajesDe(ana).size());
    }

    @Test
    void laFotoIncluyeLosBotsEliminadosDelJugador() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        assertEquals(0, ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana))
                .get(ConstantesProtocolo.CAMPO_BOTS_ELIMINADOS).getAsInt());
        ana.consultaManada().registroBotEliminado();
        assertEquals(1, ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana))
                .get(ConstantesProtocolo.CAMPO_BOTS_ELIMINADOS).getAsInt());
    }

    @Test
    void unaMuerteSeAvisaComoEventoDeMuerte() {
        SesionPartida sesion = partida();
        Jugador ana = sesion.consultaJugadores().get(0);
        MotorQueGuarda motor = new MotorQueGuarda(sesion, reloj::get);
        ManadaBots bots = botsDe(sesion);
        AnimalBot victima = (AnimalBot) bots.consultaAnimales().get(0);
        Punto centroBots = centroDeLaZonaDe(sesion, victima);
        for (Animal animal : ana.consultaManada().consultaAnimales()) {
            colocar(sesion, animal, centroBots.consultaPosicionX(), centroBots.consultaPosicionY());
        }
        motor.ejecucionOrdenAtaque(ana, ana.consultaManada().consultaAnimales(), victima);
        avanzar(motor, 50);

        boolean hayMuerte = false;
        for (Mensaje mensaje : motor.mensajesDe(ana)) {
            for (JsonElement evento : ConversorMensajes.contenidoComoObjeto(mensaje).getAsJsonArray(ConstantesProtocolo.CAMPO_EVENTOS)) {
                hayMuerte |= ConstantesProtocolo.EVENTO_MUERTE.equals(
                        evento.getAsJsonObject().get(ConstantesProtocolo.CAMPO_TIPO_EVENTO).getAsString());
            }
        }
        assertTrue(hayMuerte);
    }
}
