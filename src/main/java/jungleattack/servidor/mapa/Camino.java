package jungleattack.servidor.mapa;

//Conexión transitable entre dos zonas. Hereda
//Agrega el número del camino en mapa.json, para ubicar cosas sobre él
public class Camino extends common.model.Camino {
    private final int identificador; //Número del camino dentro del mapa

    public Camino(int identificador, Zona inicio, Zona destino, double longitud){
        super(inicio, destino, longitud);
        this.identificador = identificador;
    }

    public int consultaIdentificador(){
        return identificador;
    }

    //calcular un punto intermedio entre dos posiciones
    //0 es el centro de la zona de inicio, 1 el de la zona de destino y 0.5 la mitad
    public Punto puntoEnFraccion(double fraccion){
        common.model.Zona[] extremos = consultaExtremos();
        common.model.Punto inicio = extremos[0].consultaPosicion(); //zona inicial
        common.model.Punto destino = extremos[1].consultaPosicion(); //zona final
        //Calcula una posición intermedia
        //Resta a las coordenadas del destino, las coordenadas de inicio y multiplica esa diferencia 
        //por el valor de fraccion.
        double x = inicio.consultaPosicionX() + (destino.consultaPosicionX() - inicio.consultaPosicionX()) * fraccion;
        double y = inicio.consultaPosicionY() + (destino.consultaPosicionY() - inicio.consultaPosicionY()) * fraccion;
        return new Punto((int) Math.round(x), (int) Math.round(y)); //redondea las coordenadas calculadas
    }

    //Devuelve la zona del otro lado del camino, o null si la zona recibida no es uno de sus extremos
    public Zona zonaOpuesta(Zona zona) {
        common.model.Zona[] extremos = consultaExtremos();
        if (extremos[0] == zona) {
            return (Zona) extremos[1];
        }
        if (extremos[1] == zona) {
            return (Zona) extremos[0];
        }
        return null;
    }

}
