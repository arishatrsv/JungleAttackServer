package jungleattack.servidor.aplicacion;

import jungleattack.servidor.partidas.GestorPartidas;
import jungleattack.servidor.red.ServidorSockets;

//Punto de entrada del servidor: crea el gestor de partidas y arranca el servidor de sockets.

public class AplicacionServidor {
    public static void main(String[] args) {
        // Un solo GestorPartidas para todo el servidor
        // aquí viven todas las partidas activas.
        GestorPartidas gestorPartidas = new GestorPartidas();
        ServidorSockets servidor = new ServidorSockets(ConfiguracionServidor.PUERTO, gestorPartidas);

        // Si se cierra el programa, cierra el puerto ordenadamente.
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::cierre));

        servidor.inicio(); //bloquea hasta que el servidor se cierre
    }
}
