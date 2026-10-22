package jungleattack.servidor.reglas;

import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.ObjLongConsumer;
import jungleattack.servidor.constantes.ConstantesJuego;
import jungleattack.servidor.mapa.Mapa;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;

//Hilo que actualiza los animales de UNA zona. Hay un hilo por zona 
//En cada ciclo mueve a los animales que están en su zona y, si alguno cruzó a otra zona,
//actualiza las listas de las zonas.
public class HiloZona implements Runnable {
    private final Zona zona;
    private final Mapa mapa;
    private final ReglasMovimiento reglasMovimiento;
    private final LongSupplier reloj;
    //Qué hacer después de mover a un animal (por ejemplo, revisar si tocó una taza). Lo define el motor.
    private final Consumer<Animal> alMoverse;
    //Qué hacer con cada animal vivo en cada ciclo, se haya movido o no (por ejemplo, los ataques). Recibe el instante actual.
    private final ObjLongConsumer<Animal> alActualizar;
    private volatile boolean activo = true;

    public HiloZona(Zona zona, Mapa mapa, ReglasMovimiento reglasMovimiento, LongSupplier reloj) {
        this(zona, mapa, reglasMovimiento, reloj, animal -> { });
    }

    public HiloZona(Zona zona, Mapa mapa, ReglasMovimiento reglasMovimiento, LongSupplier reloj, Consumer<Animal> alMoverse) {
        this(zona, mapa, reglasMovimiento, reloj, alMoverse, (animal, ahora) -> { });
    }

    public HiloZona(Zona zona, Mapa mapa, ReglasMovimiento reglasMovimiento, LongSupplier reloj, Consumer<Animal> alMoverse,
            ObjLongConsumer<Animal> alActualizar) {
        this.zona = zona;
        this.mapa = mapa;
        this.reglasMovimiento = reglasMovimiento;
        this.reloj = reloj;
        this.alMoverse = alMoverse;
        this.alActualizar = alActualizar;
    }

    @Override
    public void run() {
        while (activo) {
            actualizacionZona(reloj.getAsLong());
            try {
                Thread.sleep(ConstantesJuego.INTERVALO_TICK_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    //Un ciclo: mueve a todos los animales de la zona hasta el instante "ahora"
    public void actualizacionZona(long ahora) {
        for (Animal animal : zona.animalesEnZona()) {
            if (reglasMovimiento.actualizacionPosicion(animal, ahora)) {
                mapa.actualizacionUbicacionAnimal(animal);
                alMoverse.accept(animal);
            }
            alActualizar.accept(animal, ahora);
        }
    }

    public void cierre() {
        activo = false;
    }
}
