package jungleattack.servidor.reglas;

import common.constantes.ConstantesComunes;
import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.AnimalBot;
import jungleattack.servidor.modelo.EventoCombate;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.ManadaJugador;

//Reglas del combate: Un ataque alcanza a un animal que este en el radio o menos del atacante hace tanto
//daño como tenga la manada del atacante, y cada animal puede atacar una vez por segundo.
//Si un animal de jugador elimina a un bot, se cuenta para la manada, y cada 2
//eliminaciones todos los animales vivos de esa manada ganan energía.
public class ReglasCombate {
    private final Mapa mapa;
    private final List<Manada> manadas;
    private final RegistroEventos registroEventos;
    private final ReglasObjetos reglasObjetos;

    public ReglasCombate(Mapa mapa, List<Manada> manadas, RegistroEventos registroEventos, ReglasObjetos reglasObjetos) {
        this.mapa = mapa;
        this.manadas = manadas;
        this.registroEventos = registroEventos;
        this.reglasObjetos = reglasObjetos;
    }

    // El daño de un ataque del animal
    public int calculoDanio(Animal animal) {
        return animal.consultaManada().consultaDanio();
    }

    public boolean comprobacionAlcance(Animal atacante, Animal objetivo) {
        double[] a = posicion(atacante);
        double[] b = posicion(objetivo);
        // Las diferencias en x e y son los catetos de un triángulo rectángulo entre ambos animales.
        // Math.hypot aplica Pitágoras, obteniendo la distancia.
        // Se compara con el radio de encuentro; <= incluye también el límite exacto.
        return Math.hypot(a[0] - b[0], a[1] - b[1]) <= ConstantesComunes.ENCOUNTER_RADIUS;
    }

    //Pregunta si el animal está vivo dentro de su bloque, para ver siempre lo último que escribió otro hilo.
    private boolean vivo(Animal animal) {
        synchronized (animal) {
            return animal.consultaVida();
        }
    }

    //Lee la posición de un animal mientras otro hilo podría estar moviéndolo.
    private double[] posicion(Animal animal) {
        synchronized (animal) {
            return new double[] {animal.consultaPosicion().consultaPosicionX(), animal.consultaPosicion().consultaPosicionY()};
        }
    }

    //Intenta un ataque. Ocurre solo si los dos están vivos y son de manadas distintas
    //Si ocurre, el objetivo pierde energía, se registra el evento y, si murió, se saca de su zona
    public boolean aplicacionDanio(Animal atacante, Animal objetivo, long ahora) {
        if (atacante == objetivo || atacante.consultaManada() == objetivo.consultaManada()
                || !vivo(atacante) || !vivo(objetivo)
                || !comprobacionAlcance(atacante, objetivo)
                || !atacante.intentoAtaque(ahora, ConstantesJuego.ATTACK_COOLDOWN_MS)) {
            return false;
        }
        int danio = calculoDanio(atacante);
        boolean murio = objetivo.recepcionDanio(danio);
        double[] lugar = posicion(objetivo);
        registroEventos.registro(new EventoCombate(EventoCombate.Tipo.ATAQUE, atacante.consultaIdentificador(),
                objetivo.consultaIdentificador(), atacante.consultaEspecie().consultaIdentificador(), danio,
                (int) lugar[0], (int) lugar[1], identificadorZona(atacante), identificadorZona(objetivo)));
        if (murio) {
            registroDeMuerte(atacante, objetivo, lugar);
        }
        return true;
    }

    //Lo que pasa cuando un animal muere, deja de estar en su zona, 
    //se avisa y, si era un bot, se cuenta y se premia.
    private void registroDeMuerte(Animal atacante, Animal muerto, double[] lugar) {
        Zona zona = mapa.zonaPorPosicion(muerto.consultaPosicion());
        if (zona != null) {
            zona.eliminacionAnimal(muerto);
        }
        registroEventos.registro(new EventoCombate(EventoCombate.Tipo.MUERTE, null, muerto.consultaIdentificador(),
                muerto.consultaEspecie().consultaIdentificador(), 0, (int) lugar[0], (int) lugar[1],
                zona == null ? 0 : zona.consultaIdentificador(), zona == null ? 0 : zona.consultaIdentificador()));
        if (muerto.esBot() && atacante.consultaManada() instanceof ManadaJugador manada) {
            reglasObjetos.recuperacionPorEliminaciones(manada, manada.registroBotEliminado());
        }
    }

    private int identificadorZona(Animal animal) {
        Zona zona = mapa.zonaPorPosicion(animal.consultaPosicion());
        return zona == null ? 0 : zona.consultaIdentificador();
    }

    //Lo que se revisa en cada ciclo para cada animal vivo: un bot ataca solo, y un animal de jugador sigue atacando
    //al objetivo que se le ordenó mientras siga vivo y al alcance.
    public void actualizacionAtaques(Animal animal, long ahora) {
        if (!vivo(animal)) {
            return;
        }
        if (animal instanceof AnimalBot bot) {
            ataqueAutomatico(bot, ahora);
            return;
        }
        Animal objetivo = animal.consultaObjetivoAtaque();
        if (objetivo == null) {
            return;
        }
        if (!vivo(objetivo) || !comprobacionAlcance(animal, objetivo)) {
            detencionAtaque(animal);
            return;
        }
        aplicacionDanio(animal, objetivo, ahora);
    }

    // Deja de atacar al objetivo que tenía.
    public void detencionAtaque(Animal animal) {
        animal.detencionAtaque();
    }

    //Un bot ataca solo al animal de jugador vivo más cercano que esté a su alcance.
    public void ataqueAutomatico(AnimalBot bot, long ahora) {
        Animal masCercano = null;
        double menorDistancia = Double.MAX_VALUE;
        double[] origen = posicion(bot);
        for (Manada manada : manadas) {
            if (!(manada instanceof ManadaJugador)) {
                continue;
            }
            for (Animal candidato : manada.consultaAnimales()) {
                if (!vivo(candidato)) {
                    continue;
                }
                double[] lugar = posicion(candidato);
                double distancia = Math.hypot(origen[0] - lugar[0], origen[1] - lugar[1]);
                if (distancia <= ConstantesComunes.ENCOUNTER_RADIUS && distancia < menorDistancia) {
                    menorDistancia = distancia;
                    masCercano = candidato;
                }
            }
        }
        if (masCercano != null) {
            aplicacionDanio(bot, masCercano, ahora);
        }
    }
}
