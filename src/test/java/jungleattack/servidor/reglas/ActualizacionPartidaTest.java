package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.ManadaJugador;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import org.junit.jupiter.api.Test;

class ActualizacionPartidaTest {

    @Test
    void soloIncluyeZonasConAnimalesVivosDelJugador() {
        SesionPartida sesion = mock(SesionPartida.class);
        Mapa mapa = mock(Mapa.class);
        Jugador jugador = mock(Jugador.class);
        ManadaJugador manadaJugador = mock(ManadaJugador.class);
        Manada manadaOponente = mock(Manada.class);
        Zona propia = new Zona(1, "Propia", new Punto(0, 0), List.of());
        Zona oponente = new Zona(2, "Oponente", new Punto(100, 0), List.of());
        Zona vacia = new Zona(3, "Vacia", new Punto(200, 0), List.of());
        Animal animalPropio = mock(Animal.class);
        Animal animalOponente = mock(Animal.class);
        Animal animalMuerto = mock(Animal.class);

        when(sesion.consultaMapa()).thenReturn(mapa);
        when(mapa.consultaZonas()).thenReturn(List.of(propia, oponente, vacia));
        when(jugador.consultaManada()).thenReturn(manadaJugador);
        when(animalPropio.consultaManada()).thenReturn(manadaJugador);
        when(animalPropio.consultaVida()).thenReturn(true);
        when(animalOponente.consultaManada()).thenReturn(manadaOponente);
        when(animalOponente.consultaVida()).thenReturn(true);
        when(animalMuerto.consultaManada()).thenReturn(manadaJugador);
        when(animalMuerto.consultaVida()).thenReturn(false);
        propia.registroAnimal(animalPropio);
        oponente.registroAnimal(animalOponente);
        vacia.registroAnimal(animalMuerto);

        List<Zona> visibles = new ActualizadorPartida(sesion).zonasVisibles(jugador);

        assertEquals(List.of(propia), visibles);
        assertTrue(visibles.contains(propia));
    }

    @Test
    void noDevuelveZonasSiLaPartidaAunNoTieneMapa() {
        SesionPartida sesion = mock(SesionPartida.class);
        Jugador jugador = mock(Jugador.class);
        when(sesion.consultaMapa()).thenReturn(null);

        assertTrue(new ActualizadorPartida(sesion).zonasVisibles(jugador).isEmpty());
    }
}
