package jungleattack.servidor.modelo;

import common.model.Especie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Punto;

// Grupo de animales de una misma especie. Manada por especie: dos son de los jugadores, guarda lo que cpomparten 
// todos los animales y los otros 5 son bots. El ataque lo hace cada animal, no la manada
public abstract class Manada {
    private final Especie especie;
    private final List<Animal> animales = new ArrayList<>();
    private int danio;
    private final int energiaInicial;

    protected Manada(Especie especie, int danio, int energiaInicial) {
        this.especie = especie;
        this.danio = danio;
        this.energiaInicial = energiaInicial;
    }
    //Cada tipo de manada decide qué clase de animal crea(jugador o bot)
    protected abstract Animal creacionAnimal(String identificador, Punto posicion);

    //Crea los animales de la manada repartidos alrededor del centro de la zona 
    public void creacionAnimales(int cantidad, common.model.Punto centro) {
        for (int i = 0; i < cantidad; i++) {
            String identificador = especie.consultaIdentificador() + ConstantesJuego.SEPARADOR_IDENTIFICADOR + (i + 1);
            incorporacionAnimal(creacionAnimal(identificador, posicionEnFormacion(centro, i, cantidad)));
        }
    }

    public static Punto posicionEnFormacion(common.model.Punto centro, int indice, int total) {
        double angulo = 2 * Math.PI * indice / total; // Divide el círculo completo entre "total" posiciones; "indice" determina el ángulo de este animal.
        int x = centro.consultaPosicionX() + (int) Math.round(ConstantesJuego.RADIO_FORMACION * Math.cos(angulo)); // Usa el coseno para calcular cuánto se desplaza horizontalmente desde el centro.
        int y = centro.consultaPosicionY() + (int) Math.round(ConstantesJuego.RADIO_FORMACION * Math.sin(angulo)); // Usa el seno para calcular cuánto se desplaza verticalmente desde el centro.
        return new Punto(x, y); // Forma y devuelve la posición final del animal con esos desplazamientos.
    }

    // Agrega un animal a la manada y le avisa a qué manada pertenece.
    public void incorporacionAnimal(Animal animal) {
        animales.add(animal);
        animal.asignacionManada(this);
    }

    public List<Animal> consultaAnimales() {
        return Collections.unmodifiableList(animales); //retorna una lista que no se puede modificar
    }

    public List<Animal> animalesVivos() {
        List<Animal> vivos = new ArrayList<>();
        for (Animal animal : animales) {
            if (animal.consultaVida()) {
                vivos.add(animal);
            }
        }
        return vivos;
    }

    // Devuelve el animal con ese identificador, o null si no es de esta manada.
    public Animal animalPorIdentificador(String identificador) {
        for (Animal animal : animales) {
            if (animal.consultaIdentificador().equals(identificador)) {
                return animal;
            }
        }
        return null;
    }

    public int cantidadAnimalesVivos() {
        return animalesVivos().size();
    }

    public void actualizacionManada() {
    }

    public Especie consultaEspecie() {
        return especie;
    }

    public int consultaDanio() {
        return danio;
    }

    public int consultaEnergiaInicial() {
        return energiaInicial;
    }
}
