package jungleattack.servidor.protocolo;

// Representa un mensaje que llega del cliente
public class Mensaje {

    private final String tipo;
    private final String contenido; // los datos, escritos como texto JSON

    public Mensaje(String tipo, String contenido) {
        this.tipo = tipo;
        this.contenido = contenido;
    }

    public String consultaTipo() {
        return tipo;
    }

    public String consultaContenido() {
        return contenido;
    }
}