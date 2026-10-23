package jungleattack.servidor.red;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    // Lleva a dos clientes hasta el final de la selección: ambos eligen especie y la partida empieza.
    // Deja leídos los mensajes de lobby, de modo que lo siguiente que llega a cada uno es GAME_STARTED.
    private void llegarAlInicio(ClientePrueba ana, ClientePrueba segundoJugador) throws IOException {
        ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
        String codigo = ana.leerCampo(ConstantesProtocolo.PARTIDA_CREADA, ConstantesProtocolo.CAMPO_CODIGO);
        segundoJugador.enviar(ConstantesProtocolo.UNIRSE_PARTIDA,
                ConstantesProtocolo.CAMPO_APODO, "Jugador2", ConstantesProtocolo.CAMPO_CODIGO, codigo);
        segundoJugador.leer(); // Confirmación de unión.
        segundoJugador.leer(); // Selección de especies.
        ana.leer(); // Selección de especies.
        ana.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "jaguar");
        ana.leer(); // Actualización de la selección.
        segundoJugador.leer();
        segundoJugador.enviar(ConstantesProtocolo.SELECCIONAR_ANIMAL, ConstantesProtocolo.CAMPO_ESPECIE, "tucan");
        ana.leer(); // Selección completa.
        segundoJugador.leer();
    }

    @Test
    void cuandoLosDosEligenLaPartidaEmpiezaYCadaUnoRecibeSuInicio() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            llegarAlInicio(ana, segundoJugador);

            Mensaje inicioAna = ana.leer();
            Mensaje inicioSegundo = segundoJugador.leer();
            assertEquals(ConstantesProtocolo.PARTIDA_INICIADA, inicioAna.consultaTipo());
            assertEquals(ConstantesProtocolo.PARTIDA_INICIADA, inicioSegundo.consultaTipo());

            JsonObject deAna = ConversorMensajes.contenidoComoObjeto(inicioAna);
            JsonObject deSegundo = ConversorMensajes.contenidoComoObjeto(inicioSegundo);
            // Cada jugador empieza en una zona distinta, ve solo esa zona y recibe sus 10 animales.
            assertEquals(1, deAna.getAsJsonArray(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES).size());
            assertEquals(1, deSegundo.getAsJsonArray(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES).size());
            assertTrue(deAna.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt()
                    != deSegundo.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt());
            assertEquals(10, deAna.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).size());
            assertEquals("jaguar", deAna.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).get(0)
                    .getAsJsonObject().get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
            assertEquals("tucan", deSegundo.getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES).get(0)
                    .getAsJsonObject().get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
            assertEquals(1, deAna.getAsJsonArray(ConstantesProtocolo.CAMPO_BANDERAS).size());
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void siUnJugadorSeVaConLaPartidaEnCursoElOtroGanaPorAbandono() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0])) {
            try (ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
                llegarAlInicio(ana, segundoJugador);
                ana.leer(); // GAME_STARTED
                segundoJugador.leer(); // GAME_STARTED
            } // El segundo jugador se desconecta con la partida en curso.
            Mensaje fin = ana.leer();
            assertEquals(ConstantesProtocolo.FIN_PARTIDA, fin.consultaTipo());
            JsonObject contenido = ConversorMensajes.contenidoComoObjeto(fin);
            assertEquals(ConstantesProtocolo.RESULTADO_VICTORIA, contenido.get(ConstantesProtocolo.CAMPO_RESULTADO).getAsString());
            assertEquals(ConstantesProtocolo.RAZON_ABANDONO, contenido.get(ConstantesProtocolo.CAMPO_RAZON).getAsString());
            assertEquals("Ana", contenido.get(ConstantesProtocolo.CAMPO_GANADOR).getAsString());
            assertEquals("jaguar", contenido.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_PROPIO)
                    .get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
            assertEquals("tucan", contenido.getAsJsonObject(ConstantesProtocolo.CAMPO_RESUMEN_OPONENTE)
                    .get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());

            // Ana ya no está en la partida: puede crear otra.
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            assertEquals(ConstantesProtocolo.PARTIDA_CREADA, ana.leer().consultaTipo());
        } finally {
            servidor.cierre();
        }
    }

    // Lee mensajes de un cliente hasta recibir un STATE_UPDATE donde el animal cumple la condición.
    // Falla si pasan 10 segundos. Devuelve los datos del animal en ese STATE_UPDATE.
    private JsonObject esperarAnimal(ClientePrueba cliente, String idAnimal, java.util.function.Predicate<JsonObject> condicion)
            throws IOException {
        long limite = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < limite) {
            Mensaje mensaje = cliente.leer();
            if (!ConstantesProtocolo.ACTUALIZACION_ESTADO.equals(mensaje.consultaTipo())) {
                continue;
            }
            for (JsonElement elemento : ConversorMensajes.contenidoComoObjeto(mensaje)
                    .getAsJsonArray(ConstantesProtocolo.CAMPO_ANIMALES)) {
                JsonObject animal = elemento.getAsJsonObject();
                if (idAnimal.equals(animal.get(ConstantesProtocolo.CAMPO_ID).getAsString()) && condicion.test(animal)) {
                    return animal;
                }
            }
        }
        throw new AssertionError("No llegó el STATE_UPDATE esperado para " + idAnimal);
    }

    @Test
    void moverAnimalesAntesDeEmpezarLaPartidaDaFaseIncorrecta() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            ana.leer(); // MATCH_CREATED
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_X, "100", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void lasOrdenesDeMovimientoInvalidasRecibenSuError() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            llegarAlInicio(ana, segundoJugador);
            ana.leer(); // GAME_STARTED
            segundoJugador.leer(); // GAME_STARTED

            // Un animal del oponente (tucán) no se puede mover.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "tucan-1",
                    ConstantesProtocolo.CAMPO_X, "100", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
            // Un animal que no existe.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "dragon-9",
                    ConstantesProtocolo.CAMPO_X, "100", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
            // Sin animales.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "",
                    ConstantesProtocolo.CAMPO_X, "100", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
            // Destino fuera del mapa.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_X, "5000", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_DESTINO_INVALIDO);
            // Destino que no es un número.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_X, "abc", ConstantesProtocolo.CAMPO_Y, "100");
            ana.esperarError(ConstantesProtocolo.MOTIVO_MENSAJE_INVALIDO);
            // Falta el destino.
            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1");
            ana.esperarError(ConstantesProtocolo.MOTIVO_MENSAJE_INVALIDO);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void unaOrdenDeMovimientoLlegaALosClientesComoActualizacionesDeEstado() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            llegarAlInicio(ana, segundoJugador);
            JsonObject inicio = ConversorMensajes.contenidoComoObjeto(ana.leer());
            segundoJugador.leer(); // GAME_STARTED

            // Destino: un punto del camino que sale de la zona inicial de Ana, a 80 unidades de su centro.
            jungleattack.servidor.mapa.Mapa mapa = new jungleattack.servidor.mapa.Mapa();
            mapa.generacionMapa();
            jungleattack.servidor.mapa.Zona origen = mapa.zonaPorIdentificador(
                    inicio.get(ConstantesProtocolo.CAMPO_ZONA_INICIAL).getAsInt());
            jungleattack.servidor.mapa.Zona vecina = mapa.zonasConectadas(origen).get(0);
            double dx = vecina.consultaPosicion().consultaPosicionX() - origen.consultaPosicion().consultaPosicionX();
            double dy = vecina.consultaPosicion().consultaPosicionY() - origen.consultaPosicion().consultaPosicionY();
            double largo = Math.hypot(dx, dy);
            int destinoX = (int) Math.round(origen.consultaPosicion().consultaPosicionX() + dx / largo * 80);
            int destinoY = (int) Math.round(origen.consultaPosicion().consultaPosicionY() + dy / largo * 80);

            ana.enviar(ConstantesProtocolo.MOVER_ANIMALES, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_X, String.valueOf(destinoX), ConstantesProtocolo.CAMPO_Y, String.valueOf(destinoY));

            // Llegan actualizaciones con el animal en movimiento y al final lo muestran en el destino.
            assertEquals(0, inicio.get(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS).getAsInt());
            JsonObject enCamino = esperarAnimal(ana, "jaguar-1", animal -> {
                int x = animal.get(ConstantesProtocolo.CAMPO_X).getAsInt();
                int y = animal.get(ConstantesProtocolo.CAMPO_Y).getAsInt();
                return (x != destinoX || y != destinoY) && !(x == origen.consultaPosicion().consultaPosicionX() + 30
                        && y == origen.consultaPosicion().consultaPosicionY());
            });
            assertTrue(enCamino.get(ConstantesProtocolo.CAMPO_ES_PROPIO).getAsBoolean());
            JsonObject llegada = esperarAnimal(ana, "jaguar-1", animal ->
                    Math.abs(animal.get(ConstantesProtocolo.CAMPO_X).getAsInt() - destinoX) <= 1
                            && Math.abs(animal.get(ConstantesProtocolo.CAMPO_Y).getAsInt() - destinoY) <= 1);
            assertEquals("jaguar", llegada.get(ConstantesProtocolo.CAMPO_ESPECIE).getAsString());
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void atacarAntesDeEmpezarLaPartidaDaFaseIncorrecta() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0])) {
            ana.enviar(ConstantesProtocolo.CREAR_PARTIDA, ConstantesProtocolo.CAMPO_APODO, "Ana");
            ana.leer(); // MATCH_CREATED
            ana.enviar(ConstantesProtocolo.ATACAR, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_OBJETIVO, "tucan-1");
            ana.esperarError(ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
        } finally {
            servidor.cierre();
        }
    }

    @Test
    void lasOrdenesDeAtaqueInvalidasRecibenSuError() throws Exception {
        int[] puerto = new int[1];
        ServidorSockets servidor = arrancarServidor(puerto);
        try (ClientePrueba ana = new ClientePrueba(puerto[0]); ClientePrueba segundoJugador = new ClientePrueba(puerto[0])) {
            llegarAlInicio(ana, segundoJugador);
            ana.leer(); // GAME_STARTED
            segundoJugador.leer(); // GAME_STARTED

            // Un animal que no existe.
            ana.enviar(ConstantesProtocolo.ATACAR, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_OBJETIVO, "dragon-9");
            ana.esperarError(ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO);
            // Un animal propio no se puede atacar.
            ana.enviar(ConstantesProtocolo.ATACAR, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_OBJETIVO, "jaguar-2");
            ana.esperarError(ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO);
            // El animal del oponente existe pero está en una zona que Ana no ve.
            ana.enviar(ConstantesProtocolo.ATACAR, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1",
                    ConstantesProtocolo.CAMPO_OBJETIVO, "tucan-1");
            ana.esperarError(ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO);
            // Falta el objetivo.
            ana.enviar(ConstantesProtocolo.ATACAR, ConstantesProtocolo.CAMPO_IDS_ANIMALES, "jaguar-1");
            ana.esperarError(ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO);
        } finally {
            servidor.cierre();
        }
    }
}
