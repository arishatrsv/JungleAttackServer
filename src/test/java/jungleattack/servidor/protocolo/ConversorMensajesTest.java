package jungleattack.servidor.protocolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

    @Test
    void unContenidoConListasYObjetosAnidadosSeConservaEnLaIdaYVuelta() {
        JsonObject contenido = new JsonObject();
        JsonArray zonas = new JsonArray();
        zonas.add(7);
        contenido.addProperty("zonaInicial", 7);
        contenido.add("zonasVisibles", zonas);
        Mensaje mensaje = ConversorMensajes.creacionMensajeConContenido("GAME_STARTED", contenido);
        Mensaje recuperado = ConversorMensajes.conversionDesdeJson(ConversorMensajes.conversionAJson(mensaje));

        JsonObject leido = ConversorMensajes.contenidoComoObjeto(recuperado);
        assertEquals("GAME_STARTED", recuperado.consultaTipo());
        assertEquals(7, leido.get("zonaInicial").getAsInt());
        assertEquals(1, leido.getAsJsonArray("zonasVisibles").size());
        assertTrue(leido.getAsJsonArray("zonasVisibles").get(0).getAsInt() == 7);
    }

    @Test
    void unContenidoQueNoEsUnObjetoJsonLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> ConversorMensajes.contenidoComoObjeto(new Mensaje("X", "[1,2]")));
        assertThrows(IllegalArgumentException.class,
                () -> ConversorMensajes.contenidoComoObjeto(new Mensaje("X", "no es json {")));
    }
}
