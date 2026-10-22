package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.model.Especie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalBot;
import jungleattack.servidor.modelo.AnimalJugador;
import jungleattack.servidor.modelo.EventoCombate;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.ManadaBots;
import jungleattack.servidor.modelo.ManadaJugador;
import org.junit.jupiter.api.Test;

class ReglasCombateTest {

    private final Mapa mapa = crearMapa();
    private final ManadaJugador jugadores = new ManadaJugador(Especie.JAGUAR);
    private final ManadaBots bots = new ManadaBots(Especie.COCODRILO);
    private final RegistroEventos eventos = new RegistroEventos();
    private final ReglasCombate reglas = new ReglasCombate(mapa, List.of(jugadores, bots), eventos, new ReglasObjetos());

    private static Mapa crearMapa() {
        Mapa mapa = new Mapa();
        mapa.generacionMapa();
        return mapa;
    }

    // Un animal de jugador con esa energía, en (x, y), ya registrado en su zona.
    private Animal jugador(int energia, int x, int y) {
        Animal animal = new AnimalJugador("jaguar-" + (jugadores.consultaAnimales().size() + 1), Especie.JAGUAR, energia, new Punto(x, y));
        jugadores.incorporacionAnimal(animal);
        mapa.zonaPorPosicion(animal.consultaPosicion()).registroAnimal(animal);
        return animal;
    }

    private AnimalBot bot(int energia, int x, int y) {
        AnimalBot animal = new AnimalBot("cocodrilo-" + (bots.consultaAnimales().size() + 1), Especie.COCODRILO, energia, new Punto(x, y));
        bots.incorporacionAnimal(animal);
        mapa.zonaPorPosicion(animal.consultaPosicion()).registroAnimal(animal);
        return animal;
    }

    private void mover(Animal animal, int x, int y) {
        animal.actualizacionPosicion(new Punto(x, y));
    }

    // ----- Daño y alcance -----

    @Test
    void elDanioEsElDeLaManadaYSubeConLasTazas() {
        Animal jaguar = jugador(20, 695, 420);
        AnimalBot cocodrilo = bot(10, 695, 420);
        assertEquals(2, reglas.calculoDanio(jaguar));
        assertEquals(1, reglas.calculoDanio(cocodrilo));
        jugadores.consumoTaza();
        assertEquals(3, reglas.calculoDanio(jaguar));
    }

    @Test
    void elAlcanceEsElRadioDeEncuentroYElLimiteCuenta() {
        Animal a = jugador(20, 600, 420);
        assertTrue(reglas.comprobacionAlcance(a, bot(10, 640, 420))); // exactamente a 40
        assertFalse(reglas.comprobacionAlcance(a, bot(10, 641, 420))); // a 41
        assertTrue(reglas.comprobacionAlcance(a, bot(10, 624, 452))); // diagonal: 24 y 32 dan 40
    }

    @Test
    void elAlcanceEsIgualEnLosDosSentidos() {
        Animal a = jugador(20, 600, 420);
        Animal b = bot(10, 630, 420);
        assertEquals(reglas.comprobacionAlcance(a, b), reglas.comprobacionAlcance(b, a));
    }

    // ----- Un ataque -----

    @Test
    void unAtaqueRestaLaEnergiaYRegistraElEvento() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(10, 620, 420);
        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 0));
        assertEquals(8, cocodrilo.consultaEnergia());
        List<EventoCombate> registrados = eventos.desde(0);
        assertEquals(1, registrados.size());
        EventoCombate evento = registrados.get(0);
        assertEquals(EventoCombate.Tipo.ATAQUE, evento.consultaTipo());
        assertEquals(jaguar.consultaIdentificador(), evento.consultaAtacante());
        assertEquals(cocodrilo.consultaIdentificador(), evento.consultaObjetivo());
        assertEquals("jaguar", evento.consultaEspecie());
        assertEquals(2, evento.consultaDanio());
        assertEquals(620, evento.consultaX());
    }

    @Test
    void cadaAnimalAtacaUnaVezPorSegundo() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(100, 620, 420);
        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 0));
        assertFalse(reglas.aplicacionDanio(jaguar, cocodrilo, 500));
        assertFalse(reglas.aplicacionDanio(jaguar, cocodrilo, 999));
        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 1000));
        assertEquals(96, cocodrilo.consultaEnergia());
    }

    @Test
    void unAtaqueFueraDeAlcanceNoOcurreNiGastaElEnfriamiento() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(10, 700, 420);
        assertFalse(reglas.aplicacionDanio(jaguar, cocodrilo, 0));
        assertEquals(10, cocodrilo.consultaEnergia());
        mover(cocodrilo, 620, 420);
        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 0)); // el intento de antes no gastó el enfriamiento
    }

    @Test
    void unAnimalMuertoNoAtacaNiRecibeAtaques() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(10, 620, 420);
        Animal muerto = jugador(20, 610, 420);
        muerto.recepcionDanio(100);
        assertFalse(reglas.aplicacionDanio(muerto, cocodrilo, 0));
        assertFalse(reglas.aplicacionDanio(cocodrilo, muerto, 0));
        assertEquals(10, cocodrilo.consultaEnergia());
        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 0));
    }

    @Test
    void nadieAtacaALosDeSuPropiaManadaNiASiMismo() {
        Animal a = jugador(20, 600, 420);
        Animal b = jugador(20, 610, 420);
        AnimalBot bot1 = bot(10, 600, 420);
        AnimalBot bot2 = bot(10, 610, 420);
        assertFalse(reglas.aplicacionDanio(a, b, 0));
        assertFalse(reglas.aplicacionDanio(a, a, 0));
        assertFalse(reglas.aplicacionDanio(bot1, bot2, 0)); // los bots tampoco se atacan entre sí
        assertEquals(20, b.consultaEnergia());
    }

    // ----- Muertes y eliminaciones -----

    @Test
    void unAtaqueQueMataSacaAlAnimalDeSuZonaYRegistraLaMuerte() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(2, 620, 420);
        var zona = mapa.zonaPorPosicion(cocodrilo.consultaPosicion());
        assertTrue(zona.animalesEnZona().contains(cocodrilo));

        assertTrue(reglas.aplicacionDanio(jaguar, cocodrilo, 0));

        assertFalse(cocodrilo.consultaVida());
        assertFalse(zona.animalesEnZona().contains(cocodrilo));
        List<EventoCombate> registrados = eventos.desde(0);
        assertEquals(2, registrados.size());
        assertEquals(EventoCombate.Tipo.ATAQUE, registrados.get(0).consultaTipo());
        assertEquals(EventoCombate.Tipo.MUERTE, registrados.get(1).consultaTipo());
        assertEquals(cocodrilo.consultaIdentificador(), registrados.get(1).consultaObjetivo());
    }

    @Test
    void eliminarUnBotSeCuentaParaLaManadaDelAtacante() {
        Animal jaguar = jugador(20, 600, 420);
        reglas.aplicacionDanio(jaguar, bot(2, 620, 420), 0);
        assertEquals(1, jugadores.consultaBotsEliminados());
    }

    @Test
    void quePierdaUnAnimalDeJugadorNoCuentaComoBotEliminado() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal jaguar = jugador(1, 620, 420);
        reglas.aplicacionDanio(cocodrilo, jaguar, 0);
        assertFalse(jaguar.consultaVida());
        assertEquals(0, jugadores.consultaBotsEliminados());
        assertEquals(1, jugadores.cantidadAnimalesMuertos());
    }

    @Test
    void cadaDosBotsEliminadosLosAnimalesVivosGananUnaEnergia() {
        Animal lleno = jugador(20, 600, 420);
        Animal herido = jugador(15, 600, 420);
        Animal muerto = jugador(20, 600, 420);
        muerto.recepcionDanio(100);

        reglas.aplicacionDanio(lleno, bot(2, 620, 420), 0);
        assertEquals(15, herido.consultaEnergia()); // con 1 bot eliminado todavía no hay premio

        reglas.aplicacionDanio(lleno, bot(2, 620, 420), 1000);
        assertEquals(16, herido.consultaEnergia()); // con 2 sí
        assertEquals(20, lleno.consultaEnergia()); // nunca pasa del máximo
        assertEquals(0, muerto.consultaEnergia()); // los muertos no reviven

        reglas.aplicacionDanio(lleno, bot(2, 620, 420), 2000);
        assertEquals(16, herido.consultaEnergia());
        reglas.aplicacionDanio(lleno, bot(2, 620, 420), 3000);
        assertEquals(17, herido.consultaEnergia()); // el 4.º da otro premio
        assertEquals(4, jugadores.consultaBotsEliminados());
    }

    @Test
    void variosAtacantesALaVezContabilizanLaEliminacionUnaSolaVez() throws Exception {
        for (int vuelta = 0; vuelta < 20; vuelta++) {
            ManadaJugador manadaLocal = new ManadaJugador(Especie.JAGUAR);
            ManadaBots botsLocales = new ManadaBots(Especie.COCODRILO);
            ReglasCombate reglasLocales = new ReglasCombate(mapa, List.of(manadaLocal, botsLocales), new RegistroEventos(), new ReglasObjetos());
            AnimalBot victima = new AnimalBot("cocodrilo-1", Especie.COCODRILO, 1, new Punto(600, 420));
            botsLocales.incorporacionAnimal(victima);
            mapa.zonaPorPosicion(victima.consultaPosicion()).registroAnimal(victima);
            CountDownLatch salida = new CountDownLatch(1);
            List<Thread> hilos = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Animal atacante = new AnimalJugador("jaguar-" + i, Especie.JAGUAR, 20, new Punto(610, 420));
                manadaLocal.incorporacionAnimal(atacante);
                Thread hilo = new Thread(() -> {
                    try {
                        salida.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    reglasLocales.aplicacionDanio(atacante, victima, 0);
                });
                hilo.start();
                hilos.add(hilo);
            }
            salida.countDown();
            for (Thread hilo : hilos) {
                hilo.join();
            }
            assertEquals(1, manadaLocal.consultaBotsEliminados());
        }
    }

    // ----- Ataques por orden del jugador -----

    @Test
    void unaOrdenDeAtaqueSigueGolpeandoCadaSegundo() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(100, 620, 420);
        jaguar.ordenAtaque(cocodrilo);
        reglas.actualizacionAtaques(jaguar, 0);
        reglas.actualizacionAtaques(jaguar, 500);
        reglas.actualizacionAtaques(jaguar, 1000);
        reglas.actualizacionAtaques(jaguar, 1500);
        reglas.actualizacionAtaques(jaguar, 2000);
        assertEquals(94, cocodrilo.consultaEnergia()); // 3 golpes de 2: en 0, 1000 y 2000
        assertSame(cocodrilo, jaguar.consultaObjetivoAtaque());
    }

    @Test
    void siElObjetivoMurioElAtaqueSeDetiene() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(2, 620, 420);
        jaguar.ordenAtaque(cocodrilo);
        reglas.actualizacionAtaques(jaguar, 0); // lo mata
        assertFalse(cocodrilo.consultaVida());
        reglas.actualizacionAtaques(jaguar, 1000);
        assertNull(jaguar.consultaObjetivoAtaque());
    }

    @Test
    void siElObjetivoSeAlejaElAtaqueSeDetiene() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(100, 620, 420);
        jaguar.ordenAtaque(cocodrilo);
        reglas.actualizacionAtaques(jaguar, 0);
        mover(cocodrilo, 700, 420); // se fue
        reglas.actualizacionAtaques(jaguar, 1000);
        assertNull(jaguar.consultaObjetivoAtaque());
        assertEquals(98, cocodrilo.consultaEnergia()); // solo recibió el primer golpe
        mover(cocodrilo, 620, 420); // vuelve, pero la orden ya se canceló
        reglas.actualizacionAtaques(jaguar, 2000);
        assertEquals(98, cocodrilo.consultaEnergia());
    }

    @Test
    void unAnimalSinOrdenNoAtaca() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(10, 610, 420);
        reglas.actualizacionAtaques(jaguar, 0);
        assertEquals(10, cocodrilo.consultaEnergia());
    }

    @Test
    void unAnimalMuertoNoActuaEnElCiclo() {
        Animal jaguar = jugador(20, 600, 420);
        AnimalBot cocodrilo = bot(10, 610, 420);
        jaguar.ordenAtaque(cocodrilo);
        jaguar.recepcionDanio(100);
        reglas.actualizacionAtaques(jaguar, 0);
        assertEquals(10, cocodrilo.consultaEnergia());
    }

    // ----- Bots -----

    @Test
    void unBotAtacaSoloAlAnimalDeJugadorMasCercanoAlAlcance() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal lejos = jugador(20, 635, 420); // a 35
        Animal cerca = jugador(20, 610, 420); // a 10
        reglas.actualizacionAtaques(cocodrilo, 0);
        assertEquals(19, cerca.consultaEnergia());
        assertEquals(20, lejos.consultaEnergia());
    }

    @Test
    void unBotSinNadieAlAlcanceNoHaceNada() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal lejos = jugador(20, 700, 420);
        reglas.actualizacionAtaques(cocodrilo, 0);
        assertEquals(20, lejos.consultaEnergia());
        assertTrue(eventos.desde(0).isEmpty());
    }

    @Test
    void unBotAtacaUnaVezPorSegundo() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal jaguar = jugador(20, 610, 420);
        for (long t = 0; t <= 2000; t += 50) {
            reglas.actualizacionAtaques(cocodrilo, t);
        }
        assertEquals(17, jaguar.consultaEnergia()); // golpes en 0, 1000 y 2000
    }

    @Test
    void variosBotsCercaAtacanTodosASuManera() {
        Animal jaguar = jugador(20, 600, 420);
        List<AnimalBot> varios = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            varios.add(bot(10, 600 + i * 5, 420));
        }
        for (AnimalBot bot : varios) {
            reglas.actualizacionAtaques(bot, 0);
        }
        assertEquals(16, jaguar.consultaEnergia()); // 4 golpes de 1 en un mismo segundo
    }

    @Test
    void losBotsNoSeAtacanEntreSi() {
        AnimalBot uno = bot(10, 600, 420);
        AnimalBot dos = bot(10, 610, 420);
        reglas.actualizacionAtaques(uno, 0);
        reglas.actualizacionAtaques(dos, 0);
        assertEquals(10, uno.consultaEnergia());
        assertEquals(10, dos.consultaEnergia());
    }

    @Test
    void unBotMuertoNoAtaca() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal jaguar = jugador(20, 610, 420);
        cocodrilo.recepcionDanio(100);
        reglas.actualizacionAtaques(cocodrilo, 0);
        assertEquals(20, jaguar.consultaEnergia());
    }

    @Test
    void unBotPuedeMatarAUnAnimalDeJugador() {
        AnimalBot cocodrilo = bot(10, 600, 420);
        Animal jaguar = jugador(1, 610, 420);
        reglas.actualizacionAtaques(cocodrilo, 0);
        assertFalse(jaguar.consultaVida());
        assertFalse(mapa.zonaPorPosicion(jaguar.consultaPosicion()).animalesEnZona().contains(jaguar));
    }
}
