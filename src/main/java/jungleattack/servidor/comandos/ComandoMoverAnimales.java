package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import java.util.ArrayList;
import java.util.List;
import jungleattack.servidor.mapa.Punto;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.EstadoPartida;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

//Comando cliente manda a mover los animales seleccionados a un punto del mapa.
public class ComandoMoverAnimales implements ComandoCliente {

    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        SesionPartida sesion = contexto.consultaSesion();
        Jugador jugador = contexto.consultaJugador();
        // Solo se puede mover con la partida en curso.
        if (sesion == null || jugador == null || sesion.consultaEstado() != EstadoPartida.EN_CURSO
                || sesion.consultaMotor() == null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
            return;
        }

        // El destino debe tener dos coordenadas numéricas.
        String coordenadaX = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_X);
        String coordenadaY = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_Y);

        Punto destino;

        try {
            if (coordenadaX == null || coordenadaY == null) {
                enviarError(contexto, ConstantesProtocolo.MOTIVO_MENSAJE_INVALIDO);
                return;
            }

            destino = new Punto(
                    Integer.parseInt(coordenadaX.trim()),
                    Integer.parseInt(coordenadaY.trim()));
        } catch (NumberFormatException e) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_MENSAJE_INVALIDO);
            return;
        }
        // Los animales deben existir y ser de la manada de este jugador. Los que ya
        // murieron se ignoran
        String identificadores = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_IDS_ANIMALES);
        List<Animal> animales = new ArrayList<>();
        if (identificadores != null) {
            for (String identificador : identificadores.split(ConstantesProtocolo.SEPARADOR_LISTA)) {
                if (identificador.isBlank()) { // si el identificador está vacio o solo espacios en blanco
                    continue;
                }
                Animal animal = jugador.consultaManada().animalPorIdentificador(identificador.trim());
                if (animal == null) {
                    enviarError(contexto, ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
                    return;
                }
                if (animal.consultaVida() && !animales.contains(animal)) {
                    animales.add(animal);
                }

            }
        }
        if (animales.isEmpty()) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
            return;
        }
        if (!sesion.consultaMotor().ejecucionOrdenMovimiento(jugador, animales, destino)) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_DESTINO_INVALIDO);
        }
    }

    private void enviarError(ContextoCliente contexto, String motivo) {
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
    }
}
