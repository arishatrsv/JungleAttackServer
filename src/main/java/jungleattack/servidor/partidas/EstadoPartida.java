package jungleattack.servidor.partidas;

public enum EstadoPartida {
    ESPERANDO_JUGADOR,  // Se creó la partida y falta el segundo jugador.
    SELECCION_ESPECIE, //Están los dos jugadores y cada uno elige su especie.
    EN_CURSO,// La partida está en juego.
    FINALIZADA // Alguien ganó o se canceló la partida.
}
