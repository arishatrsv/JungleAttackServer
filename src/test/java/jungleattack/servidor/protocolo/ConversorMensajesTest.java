package jungleattack.servidor.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConversorMensajesTest {

    @Test
    void idaYVueltaConservaTipoYContenido() {
        Mensaje original = new Mensaje("CREATE_MATCH", "{\"apodo\":\"Ana\"}");
        String json = ConversorMensajes.conversionAJson(original);
        Mensaje recuperado = ConversorMensajes.conversionDesdeJson(json);
        assertEquals("CREATE_MATCH", recuperado.consultaTipo());
        assertEquals("{\"apodo\":\"Ana\"}", recuperado.consultaContenido());
    }

    @Test
    void creacionMensajeArmaElContenidoConLosCampos() {
        Mensaje mensaje = ConversorMensajes.creacionMensaje("MATCH_CREATED", "codigo", "A7K2");
        assertEquals("MATCH_CREATED", mensaje.consultaTipo());
        assertEquals("A7K2", ConversorMensajes.campoDelContenido(mensaje, "codigo"));
    }

    @Test
    void campoQueNoExisteDevuelveNull() {
        Mensaje mensaje = ConversorMensajes.creacionMensaje("X", "a", "1");
        assertNull(ConversorMensajes.campoDelContenido(mensaje, "b"));
    }

    @Test
    void contenidoMalFormadoDevuelveNull() {
        Mensaje mensaje = new Mensaje("X", "esto no es json");
        assertNull(ConversorMensajes.campoDelContenido(mensaje, "a"));
    }

    @Test
    void unTextoQueNoEsJsonLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> ConversorMensajes.conversionDesdeJson("hola"));
    }

    @Test
    void unMensajeSinTipoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> ConversorMensajes.conversionDesdeJson("{\"contenido\":\"{}\"}"));
    }

    @Test
    void sinContenidoQuedaComoObjetoVacio() {
        Mensaje mensaje = ConversorMensajes.conversionDesdeJson("{\"tipo\":\"X\"}");
        assertEquals("{}", mensaje.consultaContenido());
    }
}
