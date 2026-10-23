package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import common.constantes.ConstantesProtocolo;
import common.model.Especie;
import java.util.Random;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;
import org.junit.jupiter.api.Test;

class ActualizadorPartidaTest {

    // Partida ya iniciada con Ana (jaguar) y Luis (tucán).
    private SesionPartida partida() {
        SesionPartida sesion = new SesionPartida("AB12", new Random(4), false);
        Jugador ana = new Jugador("Ana", null);
        Jugador luis = new Jugador("Luis", null);
        sesion.registroJugador(ana);
        sesion.registroJugador(luis);
        sesion.eleccionEspecie(ana, Especie.JAGUAR);
        sesion.eleccionEspecie(luis, Especie.TUCAN);
        sesion.inicio();
        return sesion;
    }

    private JsonObject contenidoInicio(ActualizadorPartida actualizador, Jugador jugador) {
        Mensaje mensaje = actualizador.mensajeInicio(jugador);
        assertEquals(ConstantesProtocolo.PARTIDA_INICIADA, mensaje.consultaTipo());
        return ConversorMensajes.contenidoComoObjeto(mensaje);
    }

    @Test
    void alEmpezarCadaJugadorSoloVeSuPropiaZona() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        for (Jugador jugador : sesion.consultaJugadores()) {
            assertEquals(1, actualizador.zonasVisibles(jugador).size());
        }
        Zona zonaAna = actualizador.zonasVisibles(sesion.consultaJugadores().get(0)).get(0);
        Zona zonaLuis = actualizador.zonasVisibles(sesion.consultaJugadores().get(1)).get(0);
        assertFalse(zonaAna == zonaLuis);
    }

    @Test
    void unJugadorSinManadaNoVeNingunaZona() {
        assertTrue(new ActualizadorPartida(partida()).zonasVisibles(new Jugador("Otro", null)).isEmpty());
    }

    @Test
    void elMensajeDeInicioTraeLaZonaLaManadaYLaBanderaPropia() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        JsonObject contenido = contenidoInicio(actualizador, ana);

        int zonaInicial = contenido.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt();
        assertEquals(actualizador.zonasVisibles(ana).get(0).consultaIdentificador(), zonaInicial);
        assertEquals(2, contenido.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());

        JsonArray zonasVisibles = contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES);
        assertEquals(1, zonasVisibles.size());
        assertEquals(zonaInicial, zonasVisibles.get(0).getAsInt());

        JsonArray animales = contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES);
        assertEquals(10, animales.size());
        for (JsonElement elemento : animales) {
            JsonObject animal = elemento.getAsJsonObject();
            assertEquals("jaguar", animal.get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
            assertEquals(20, animal.get(ConstantesProtocolo.CAMPO_ENERGIA).getAsInt());
            assertEquals(20, animal.get(ConstantesProtocolo.CAMPO_ENERGIA_MAXIMA).getAsInt());
            assertTrue(animal.get(ConstantesProtocolo.CAMPO_ES_PROPIO).getAsBoolean());
            assertFalse(animal.get(ConstantesProtocolo.CAMPO_ES_BOT).getAsBoolean());
            assertTrue(animal.has(ConstantesProtocolo.CAMPO_ID));
            assertTrue(animal.has(ConstantesProtocolo.CAMPO_X));
            assertTrue(animal.has(ConstantesProtocolo.CAMPO_Y));
        }

        JsonArray banderas = contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_BANDERAS);
        assertEquals(1, banderas.size());
        assertTrue(banderas.get(0).getAsJsonObject().get(ConstantesProtocolo.CAMPO_ES_PROPIO).getAsBoolean());
        assertEquals(zonaInicial, banderas.get(0).getAsJsonObject().get(ConstantesProtocolo.CAMPO_ZONA).getAsInt());
    }

    @Test
    void lasTazasDelMensajeSonSoloLasDeLasZonasVisibles() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        for (Jugador jugador : sesion.consultaJugadores()) {
            JsonObject contenido = contenidoInicio(actualizador, jugador);
            int zonaInicial = contenido.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt();
            JsonArray tazas = contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_TAZAS);
            int esperadas = actualizador.zonasVisibles(jugador).get(0).tazasEnZona().size();
            assertEquals(esperadas, tazas.size());
            for (JsonElement taza : tazas) {
                assertEquals(zonaInicial, taza.getAsJsonObject().get(ConstantesProtocolo.CAMPO_ZONA).getAsInt());
            }
        }
    }

    @Test
    void cadaJugadorRecibeSuPropiaZonaYNoLaDelOponente() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        JsonObject deAna = contenidoInicio(actualizador, sesion.consultaJugadores().get(0));
        JsonObject deLuis = contenidoInicio(actualizador, sesion.consultaJugadores().get(1));
        assertFalse(deAna.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt()
                == deLuis.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt());
        assertEquals("tucan", deLuis.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).get(0)
                .getAsJsonObject().get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
    }

    @Test
    void siUnAnimalAjenoEntraEnUnaZonaVisibleSeIncluyeComoNoPropio() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        Zona zonaAna = actualizador.zonasVisibles(ana).get(0);

        // Un animal de Luis aparece en la zona de Ana.
        Animal visitante = luis.consultaManada().consultaAnimales().get(0);
        visitante.actualizacionPosicion(zonaAna.consultaPosicion());
        sesion.consultaMapa().actualizacionUbicacionAnimal(visitante);

        JsonArray animales = contenidoInicio(actualizador, ana).getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES);
        assertEquals(11, animales.size());
        int ajenos = 0;
        for (JsonElement elemento : animales) {
            JsonObject animal = elemento.getAsJsonObject();
            if (!animal.get(ConstantesProtocolo.CAMPO_ES_PROPIO).getAsBoolean()) {
                ajenos++;
                assertFalse(animal.get(ConstantesProtocolo.CAMPO_ES_BOT).getAsBoolean());
                assertEquals("tucan", animal.get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
            }
        }
        assertEquals(1, ajenos);
        // Luis ahora ve dos zonas: la suya y la de Ana, donde tiene a ese animal.
        assertEquals(2, actualizador.zonasVisibles(luis).size());
    }

    @Test
    void unaZonaDejaDeSerVisibleCuandoMuereTodaLaManadaQueHabiaEnElla() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        for (Animal animal : ana.consultaManada().consultaAnimales()) {
            animal.actualizacionEnergia(0);
        }
        assertTrue(actualizador.zonasVisibles(ana).isEmpty());
    }

    @Test
    void elEstadoTraeLoMismoQueElInicioPeroConOtroTipoYSinZonaInicial() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        Mensaje estado = actualizador.mensajeEstado(ana);
        assertEquals(ConstantesProtocolo.ACTUALIZACION_ESTADO, estado.consultaTipo());
        JsonObject contenido = ConversorMensajes.contenidoComoObjeto(estado);
        assertFalse(contenido.has(ConstantesProtocolo.CAMPO_ZONA_INICIAL));
        assertEquals(1, contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES).size());
        assertEquals(10, contenido.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).size());
        assertEquals(2, contenido.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());
    }

    @Test
    void dosFotosSeguidasSinCambiosSonIguales() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        assertEquals(actualizador.mensajeEstado(ana).consultaContenido(), actualizador.mensajeEstado(ana).consultaContenido());
    }

    @Test
    void siUnAnimalSeMueveLaFotoCambia() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        String antes = actualizador.mensajeEstado(ana).consultaContenido();
        Animal animal = ana.consultaManada().consultaAnimales().get(0);
        animal.actualizacionPosicion(new jungleattack.servidor.mapa.Punto(
                animal.consultaPosicion().consultaPosicionX() + 3, animal.consultaPosicion().consultaPosicionY()));
        assertFalse(antes.equals(actualizador.mensajeEstado(ana).consultaContenido()));
    }

    @Test
    void unAnimalEnDosZonasALaVezSoloAparecePorUnaVez() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        Animal animal = ana.consultaManada().consultaAnimales().get(0);
        // Se registra a la fuerza en una segunda zona, como pasa un instante mientras cruza de una zona a otra.
        Zona otra = sesion.consultaMapa().zonasConectadas(actualizador.zonasVisibles(ana).get(0)).get(0);
        otra.registroAnimal(animal);
        JsonArray animales = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana))
                .getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES);
        assertEquals(10, animales.size());
    }

    @Test
    void laFotoIncluyeLasTazasConsumidasYElDanioActualizado() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        JsonObject antes = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana));
        assertEquals(0, antes.get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
        assertEquals(2, antes.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());

        ana.consultaManada().consumoTaza();

        JsonObject despues = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(ana));
        assertEquals(1, despues.get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
        assertEquals(3, despues.get(ConstantesProtocolo.CAMPO_DANIO).getAsInt());
        // Cada jugador ve solo sus propias tazas consumidas.
        Jugador luis = sesion.consultaJugadores().get(1);
        assertEquals(0, ConversorMensajes.contenidoComoObjeto(actualizador.mensajeEstado(luis))
                .get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
    }

    @Test
    void consumirUnaTazaCambiaLaFotoYPorEsoSeEnviaUnaActualizacion() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        String antes = actualizador.mensajeEstado(ana).consultaContenido();
        ana.consultaManada().consumoTaza();
        assertFalse(antes.equals(actualizador.mensajeEstado(ana).consultaContenido()));
    }

    @Test
    void elMensajeDeFinTraeElResultadoYElResumenDeCadaPerspectiva() {
        SesionPartida sesion = partida();
        ActualizadorPartida actualizador = new ActualizadorPartida(sesion);
        Jugador ana = sesion.consultaJugadores().get(0);
        Jugador luis = sesion.consultaJugadores().get(1);
        ana.consultaManada().registroBotEliminado();
        ana.consultaManada().registroBotEliminado();
        ana.consultaManada().consumoTaza();
        luis.consultaManada().consultaAnimales().get(0).recepcionDanio(100);
        luis.consultaManada().consultaAnimales().get(1).recepcionDanio(100);

        assertTrue(sesion.finalizacion(ana, ConstantesProtocolo.RAZON_BANDERA));

        Mensaje paraAna = actualizador.mensajeFin(ana);
        assertEquals(ConstantesProtocolo.FIN_PARTIDA, paraAna.consultaTipo());
        JsonObject deAna = ConversorMensajes.contenidoComoObjeto(paraAna);
        assertEquals(ConstantesProtocolo.RESULTADO_VICTORIA, deAna.get(ConstantesProtocolo.CAMPO_RESULTADO).getAsString());
        assertEquals("Ana", deAna.get(ConstantesProtocolo.CAMPO_GANADOR).getAsString());
        assertEquals(ConstantesProtocolo.RAZON_BANDERA, deAna.get(ConstantesProtocolo.CAMPO_RAZON).getAsString());
        assertTrue(deAna.get(ConstantesProtocolo.CAMPO_DURACION).getAsInt() >= 0);
        JsonObject propio = deAna.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_PROPIO);
        assertEquals("Ana", propio.get(ConstantesProtocolo.CAMPO_APODO).getAsString());
        assertEquals("jaguar", propio.get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
        assertEquals(2, propio.get(ConstantesProtocolo.CAMPO_BOTS_ELIMINADOS).getAsInt());
        assertEquals(0, propio.get(ConstantesProtocolo.CAMPO_ANIMALES_PERDIDOS).getAsInt());
        assertEquals(1, propio.get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
        JsonObject oponente = deAna.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_OPONENTE);
        assertEquals("Luis", oponente.get(ConstantesProtocolo.CAMPO_APODO).getAsString());
        assertEquals(2, oponente.get(ConstantesProtocolo.CAMPO_ANIMALES_PERDIDOS).getAsInt());

        // Luis recibe el mismo resumen pero con su propia perspectiva: perdió, y "propio" son sus datos.
        JsonObject deLuis = ConversorMensajes.contenidoComoObjeto(actualizador.mensajeFin(luis));
        assertEquals(ConstantesProtocolo.RESULTADO_DERROTA, deLuis.get(ConstantesProtocolo.CAMPO_RESULTADO).getAsString());
        assertEquals("Ana", deLuis.get(ConstantesProtocolo.CAMPO_GANADOR).getAsString());
        assertEquals("Luis", deLuis.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_PROPIO).get(ConstantesProtocolo.CAMPO_APODO).getAsString());
        assertEquals(2, deLuis.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_PROPIO).get(ConstantesProtocolo.CAMPO_ANIMALES_PERDIDOS).getAsInt());
        assertEquals(2, deLuis.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_OPONENTE).get(ConstantesProtocolo.CAMPO_BOTS_ELIMINADOS).getAsInt());
    }
}
