package jungleattack.servidor.red;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.constantes.ConstantesProtocolo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;
import org.junit.jupiter.api.Test;

// Pruebas de integración del servidor, la comunicación por sockets, el procesador y los comandos.
class ServidorIntegracionTest {

    // Cliente de prueba que envía mensajes y lee respuestas con un tiempo máximo de espera.
    private static class ClientePrueba implements AutoCloseable {
        private final Socket socket;
        private final PrintWriter salida;
        private final BufferedReader entrada;

        ClientePrueba(int puerto) throws IOException {
            socket = new Socket("localhost", puerto);
            socket.setSoTimeout(3000);
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        }

        void enviar(String tipo, String... camposYValores) {
            salida.println(ConversorMensajes.conversionAJson(
                    ConversorMensajes.creacionMensaje(tipo, camposYValores)));
        }

        Mensaje leer() throws IOException {
            return ConversorMensajes.conversionDesdeJson(entrada.readLine());
        }

        String leerCampo(String tipoEsperado, String campo) throws IOException {
            Mensaje mensaje = leer();
            assertEquals(tipoEsperado, mensaje.consultaTipo());
            return ConversorMensajes.campoDelContenido(mensaje, campo);
        }

        void esperarError(String motivo) throws IOException {
            assertEquals(motivo, leerCampo(ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO));
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    // Busca un puerto libre, inicia el servidor y espera a que esté listo para recibir clientes.
    private ServidorSockets arrancarServidor(int[] puertoUsado) throws Exception {
        try (ServerSocket libre = new ServerSocket(0)) {
            puertoUsado[0] = libre.getLocalPort();
        }
        ServidorSockets servidor = new ServidorSockets(puertoUsado[0], new GestorPartidas());
        Thread hilo = new Thread(servidor::inicio);
        hilo.setDaemon(true);
        hilo.start();
        for (int intento = 0; intento < 50; intento++) { // Espera hasta 5 segundos.
            try (Socket prueba = new Socket("localhost", puertoUsado[0])) {
                return servidor;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("El servidor no arrancó");
    }

    @Test
    void dosJugadoresCreanYseUnenAUnaPartida() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            String codigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);
            assertEquals(4, codigo.length());

            // El segundo jugador se une con el código en minúsculas.
            segundoJugador.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                    ConstantesProtocolo.CAMPO_APODO, "Jugador2",
                    ConstantesProtocolo.CAMPO_CODIGO, codigo.toLowerCase());
            assertEquals(codigo, segundoJugador.leerCampo(ConstantesProtocolo.PARTIDA_UNIDA, ConstantesProtocolo.CAMPO_CODIGO));

            // Ambos jugadores reciben las siete especies disponibles.
            String disponiblesSegundoJugador = segundoJugador.leerCampo(ConstantesProtocolo.SELECCION_ANIMALES, ConstantesProtocolo.CAMPO_DISPONIBLES);
            String disponiblesAna = ana.leerCampo(ConstantesProtocolo.SELECCION_ANIMALES, ConstantesProtocolo.CAMPO_DISPONIBLES);
            assertEquals(7, disponiblesAna.split(ConstantesProtocolo.SEPARADOR_LISTA).length);
            assertEquals(disponiblesAna, disponiblesSegundoJugador);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void unirseConUnCodigoQueNoExisteDaError() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba cliente = new ClientePrueba(puerto[0])) {
            cliente.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                    ConstantesProtocolo.CAMPO_APODO, "Ana", ConstantesProtocolo.CAMPO_CODIGO, "ZZZZ");
            cliente.esperarError(ConstantesProtocolo.MOTIVO_PARTIDA_NO_ENCONTRADA);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void unTercerJugadorEncuentraLaPartidaLlena() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]);
                ClientePrueba segundoJugador = new ClientePrueba(puerto[0]);
                ClientePrueba luis = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            String codigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);
            segundoJugador.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                    ConstantesProtocolo.CAMPO_APODO, "Jugador2", ConstantesProtocolo.CAMPO_CODIGO, codigo);
            segundoJugador.leer(); // Confirmación de unión.

            luis.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                    ConstantesProtocolo.CAMPO_APODO, "Luis", ConstantesProtocolo.CAMPO_CODIGO, codigo);
            luis.esperarError(ConstantesProtocolo.MOTIVO_PARTIDA_LLENA);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void laSeleccionDeEspecieNoDejaRepetirAnimal() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            String codigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);
            segundoJugador.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                    ConstantesProtocolo.CAMPO_APODO, "Jugador2", ConstantesProtocolo.CAMPO_CODIGO, codigo);
            segundoJugador.leer(); // Confirmación de unión.
            segundoJugador.leer(); // Selección de especies.
            ana.leer(); // Selección de especies.

            // El primer jugador elige jaguar y ambos reciben la actualización.
            ana.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "jaguar");
            assertEquals("jaguar", ana.leerCampo(ConstantesProtocolo.SELECCION_ANIMALES, ConstantesProtocolo.CAMPO_PROPIA));
            assertEquals("jaguar", segundoJugador.leerCampo(ConstantesProtocolo.SELECCION_ANIMALES, ConstantesProtocolo.CAMPO_OPONENTE));

            // El segundo jugador intenta elegir una especie que ya está ocupada.
            segundoJugador.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "jaguar");
            segundoJugador.esperarError(ConstantesProtocolo.MOTIVO_ESPECIE_NO_DISPONIBLE);

            // También se rechaza una especie inexistente.
            segundoJugador.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "dragon");
            segundoJugador.esperarError(ConstantesProtocolo.MOTIVO_ESPECIE_INVALIDA);

            // El segundo jugador elige otra especie y completa la selección.
            segundoJugador.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "tucan");
            assertEquals("true", segundoJugador.leerCampo(ConstantesProtocolo.SELECCION_ANIMALES, ConstantesProtocolo.CAMPO_COMPLETA));
            ana.leer(); // Actualización de la selección.
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void elegirEspecieSinEstarEnUnaPartidaDaError() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba cliente = new ClientePrueba(puerto[0])) {
            cliente.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "jaguar");
            cliente.esperarError(ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void siElOponenteSeDesconectaLaPartidaSeCancelaYSePuedeCrearOtra() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            String codigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);

            try (ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
                segundoJugador.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                        ConstantesProtocolo.CAMPO_APODO, "Jugador2", ConstantesProtocolo.CAMPO_CODIGO, codigo);
                ana.leer(); // ANIMAL_SELECTION
            } // El segundo jugador se desconecta.

            ana.esperarError(ConstantesProtocolo.MOTIVO_OPONENTE_DESCONECTADO);

            // Ana quedó libre: puede crear una partida nueva.
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            String nuevoCodigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);
            assertNotNull(nuevoCodigo);
            assertTrue(nuevoCodigo.length() == 4);
        } finally {
            servidor.cierre();
        }
    }
}
