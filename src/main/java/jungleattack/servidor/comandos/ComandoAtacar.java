package jungleattack.servidor.comandos;

import common.constantes.ConstantesProtocolo;
import java.util.ArrayList;
import java.util.List;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.partidas.EstadoPartida;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

//Comando del cliente que manda atacar a un animal con los animales seleccionados.
//El mensaje trae los identificadores de los atacantes(separado por comas) y el identificador del objetivo
//Este comando solo revisa que la orden tenga sentido y se la entrega al motor del juego, que decide
//quién está al alcance.
public class ComandoAtacar implements ComandoCliente {

    @Override
    public void ejecucion(Mensaje mensaje, ContextoCliente contexto) {
        SesionPartida sesion = contexto.consultaSesion();
        Jugador jugador = contexto.consultaJugador();
        if (sesion == null || jugador == null || sesion.consultaEstado() != EstadoPartida.EN_CURSO
                || sesion.consultaMotor() == null) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_FASE_INCORRECTA);
            return;
        }

        // El objetivo debe ser un animal de la partida, vivo y que no sea del propio jugador.
        String identificadorObjetivo = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_OBJETIVO);
        Animal objetivo = identificadorObjetivo == null ? null : sesion.animalPorIdentificador(identificadorObjetivo.trim());
        if (objetivo == null || !objetivo.consultaVida() || objetivo.consultaManada() == jugador.consultaManada()) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO);
            return;
        }

        // Los atacantes deben ser animales de este jugador. Los que ya murieron se ignoran.
        String identificadores = ConversorMensajes.campoDelContenido(mensaje, ConstantesProtocolo.CAMPO_IDS_ANIMALES);
        List<Animal> atacantes = new ArrayList<>();
        if (identificadores != null) {
            for (String identificador : identificadores.split(ConstantesProtocolo.SEPARADOR_LISTA)) {
                if (identificador.isBlank()) {
                    continue;
                }
                Animal animal = jugador.consultaManada().animalPorIdentificador(identificador.trim());
                if (animal == null) {
                    enviarError(contexto, ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
                    return;
                }
                if (animal.consultaVida() && !atacantes.contains(animal)) {
                    atacantes.add(animal);
                }
            }
        }
        if (atacantes.isEmpty()) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_ANIMAL_INVALIDO);
            return;
        }

        int atacando = sesion.consultaMotor().ejecucionOrdenAtaque(jugador, atacantes, objetivo);
        if (atacando < 0) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_OBJETIVO_INVALIDO); // por ejemplo, está en una zona que no se ve
        } else if (atacando == 0) {
            enviarError(contexto, ConstantesProtocolo.MOTIVO_FUERA_DE_ALCANCE);
        }
    }

    private void enviarError(ContextoCliente contexto, String motivo) {
        contexto.envioMensaje(ConversorMensajes.creacionMensaje(
                ConstantesProtocolo.ERROR, ConstantesProtocolo.CAMPO_MOTIVO, motivo));
    }
}
