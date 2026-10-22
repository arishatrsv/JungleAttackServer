package jungleattack.servidor.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.modelo.EventoCombate;
import org.junit.jupiter.api.Test;

class RegistroEventosTest {

    private EventoCombate evento(String objetivo) {
        return new EventoCombate(EventoCombate.Tipo.ATAQUE, "jaguar-1", objetivo, "jaguar", 2, 10, 20, 1, 2);
    }

    @Test
    void alPrincipioNoHayEventos() {
        RegistroEventos registro = new RegistroEventos();
        assertEquals(0, registro.ultimaSecuencia());
        assertTrue(registro.desde(0).isEmpty());
    }

    @Test
    void cadaEventoRecibeUnNumeroDeSecuenciaCreciente() {
        RegistroEventos registro = new RegistroEventos();
        assertEquals(1, registro.registro(evento("a")));
        assertEquals(2, registro.registro(evento("b")));
        assertEquals(2, registro.ultimaSecuencia());
    }

    @Test
    void desdeDevuelveSoloLosEventosPosteriores() {
        RegistroEventos registro = new RegistroEventos();
        registro.registro(evento("a"));
        registro.registro(evento("b"));
        registro.registro(evento("c"));
        List<EventoCombate> posteriores = registro.desde(1);
        assertEquals(2, posteriores.size());
        assertEquals("b", posteriores.get(0).consultaObjetivo());
        assertEquals("c", posteriores.get(1).consultaObjetivo());
        assertTrue(registro.desde(3).isEmpty());
    }

    @Test
    void desdeCeroDevuelveTodos() {
        RegistroEventos registro = new RegistroEventos();
        registro.registro(evento("a"));
        registro.registro(evento("b"));
        assertEquals(2, registro.desde(0).size());
    }

    @Test
    void soloSeGuardanLosUltimosEventos() {
        RegistroEventos registro = new RegistroEventos();
        int total = ConstantesJuego.MAXIMO_EVENTOS_GUARDADOS + 10;
        for (int i = 1; i <= total; i++) {
            registro.registro(evento("e" + i));
        }
        assertEquals(total, registro.ultimaSecuencia());
        List<EventoCombate> todos = registro.desde(0);
        assertEquals(ConstantesJuego.MAXIMO_EVENTOS_GUARDADOS, todos.size());
        assertEquals("e11", todos.get(0).consultaObjetivo()); // los 10 más viejos se descartaron
        assertEquals("e" + total, todos.get(todos.size() - 1).consultaObjetivo());
        // Quien ya había visto hasta un evento todavía guardado recibe exactamente los siguientes.
        assertEquals(1, registro.desde(total - 1).size());
    }
}
