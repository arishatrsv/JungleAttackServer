package jungleattack.servidor.modelo;

//Algo que pasó en un combate y que los clientes deben mostrar: un ataque
//(para dibujar el efecto sobre el animal atacado) o una muerte.
public class EventoCombate {
    public enum Tipo { ATAQUE, MUERTE }

    private final Tipo tipo;
    private final String atacante; //identificador del animal que atacó 
    private final String objetivo; //identificador del animal atacado o que murió
    private final String especie; //identificador de la especie del atacante 
    private final int danio;
    private final int x; //dónde estaba el objetivo
    private final int y;
    private final int zonaAtacante; //zonas donde ocurrió: solo ve el evento quien ve alguna de ellas
    private final int zonaObjetivo;
 
    public EventoCombate(Tipo tipo, String atacante, String objetivo, String especie, int danio, int x, int y,
        int zonaAtacante, int zonaObjetivo) {
        this.tipo = tipo;
        this.atacante = atacante;
        this.objetivo = objetivo;
        this.especie = especie;
        this.danio = danio;
        this.x = x;
        this.y = y;
        this.zonaAtacante = zonaAtacante;
        this.zonaObjetivo = zonaObjetivo;
    }

    public Tipo consultaTipo() {
        return tipo;
    }

    public String consultaAtacante() {
        return atacante;
    }

    public String consultaObjetivo() {
        return objetivo;
    }

    public String consultaEspecie() {
        return especie;
    }

    public int consultaDanio() {
        return danio;
    }

    public int consultaX() {
        return x;
    }

    public int consultaY() {
        return y;
    }

    public int consultaZonaAtacante() {
        return zonaAtacante;
    }

    public int consultaZonaObjetivo() {
        return zonaObjetivo;
    }
}
