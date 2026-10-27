package jungleattack.servidor.red;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.constantes.ConstantesProtocolo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import jungleattack.servidor.comandos.ComandoCliente;
import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.protocolo.Mensaje;
import org.junit.jupiter.api.Test;

// Usa una conexión real por la red local (sin pasar por ServidorSockets).
class ManejadorClienteTest {

    private static final int ESPERA_MAXIMA_MS = 3000;

    @Test
    void unComandoQueFallaNoCortaLaConexionYElClienteRecibeUnError() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0);
                Socket cliente = new Socket("localhost", servidor.getLocalPort());
                Socket aceptado = servidor.accept()) {
            cliente.setSoTimeout(ESPERA_MAXIMA_MS);
            ManejadorCliente manejador = new ManejadorCliente(aceptado, new GestorPartidas());
            ComandoCliente queFalla = (mensaje, contexto) -> {
                throw new IllegalStateException("fallo de prueba");
            };

            manejador.ejecucionProtegida(queFalla, new Mensaje("CUALQUIERA", "{}")); // no debe lanzar nada

            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            String linea = entrada.readLine();
            assertTrue(linea.contains(ConstantesProtocolo.ERROR));
            assertTrue(linea.contains(ConstantesProtocolo.MOTIVO_ERROR_INTERNO));
        }
    }

    @Test
    void despuesDeUnFalloElSiguienteComandoSiSeEjecuta() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0);
                Socket cliente = new Socket("localhost", servidor.getLocalPort());
                Socket aceptado = servidor.accept()) {
            ManejadorCliente manejador = new ManejadorCliente(aceptado, new GestorPartidas());
            manejador.ejecucionProtegida((mensaje, contexto) -> {
                throw new IllegalStateException("fallo de prueba");
            }, new Mensaje("PRIMERO", "{}"));

            int[] ejecutados = {0};
            manejador.ejecucionProtegida((mensaje, contexto) -> ejecutados[0]++, new Mensaje("SEGUNDO", "{}"));

            assertEquals(1, ejecutados[0]); // el semáforo se liberó aunque el primero falló
        }
    }
}
