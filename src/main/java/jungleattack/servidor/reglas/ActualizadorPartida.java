package jungleattack.servidor.reglas;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import common.constantes.ConstantesProtocolo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jungleattack.servidor.mapa.Zona;
import jungleattack.servidor.modelo.Animal;
import jungleattack.servidor.modelo.Bandera;
import jungleattack.servidor.modelo.Manada;
import jungleattack.servidor.modelo.Taza;
import jungleattack.servidor.partidas.Jugador;
import jungleattack.servidor.partidas.SesionPartida;
import jungleattack.servidor.protocolo.ConversorMensajes;
import jungleattack.servidor.protocolo.Mensaje;

//Prepara lo que se le comunica a cada cliente. El servidor solo envía lo que el jugador tiene permitido ver:
//una zona se ve únicamente si al menos un animal vivo del jugador está en ella, y de esa zona se envía todo su contenido. 
public class ActualizadorPartida {
    private final SesionPartida sesion;
    public ActualizadorPartida(SesionPartida sesion) {
        this.sesion = sesion;
    }

    //Devuelve las zonas que el jugador puede ver ahora, aquellas donde tiene al menos un animal vivo.
    //Al empezar la partida es solo la zona donde está su manada.
    public List<Zona> zonasVisibles(Jugador jugador) {
        List<Zona> visibles = new ArrayList<>();
        Manada manada = jugador.consultaManada();
        if (manada == null || sesion.consultaMapa() == null) {
            return visibles;
        }
        for (Zona zona : sesion.consultaMapa().consultaZonas()) {
            for (Animal animal : zona.animalesEnZona()) {
                if (animal.consultaManada() == manada && animal.consultaVida()) {
                    visibles.add(zona);
                    break;
                }
            }
        }
        return visibles;
    }
    
    //Arma el mensaje GAME_STARTED de un jugador. Cada jugador recibe el suyo, con: la zona donde empieza, 
    //el daño de su manada, las zonas que puede ver y el contenido de esas zonas.
    public Mensaje mensajeInicio(Jugador jugador) {
        JsonObject contenido = mostrarFoto(jugador);
        contenido.addProperty(ConstantesProtocolo.CAMPO_ZONA_INICIAL, zonaInicial(jugador).consultaIdentificador());
        return ConversorMensajes.creacionMensajeConContenido(ConstantesProtocolo.PARTIDA_INICIADA, contenido);
    }

    //Arma el mensaje STATE_UPDATE de un jugador: una foto de todo lo que puede ver en este momento 
    //las zonas que aparecen o desaparecen de su vista quedan reflejadas sin mensajes especiales
    public Mensaje mensajeEstado(Jugador jugador) {
        return ConversorMensajes.creacionMensajeConContenido(ConstantesProtocolo.ACTUALIZACION_ESTADO, mostrarFoto(jugador));
    }

    //La foto de lo que el jugador ve. 
    private JsonObject mostrarFoto(Jugador jugador) {
        Manada manada = jugador.consultaManada();
        List<Zona> visibles = zonasVisibles(jugador);
        JsonObject contenido = new JsonObject();
        contenido.addProperty(ConstantesProtocolo.CAMPO_DANIO, manada.consultaDanio());
        contenido.addProperty(ConstantesProtocolo.CAMPO_TAZAS_CONSUMIDAS, jugador.consultaManada().consultaTazasConsumidas());

        JsonArray identificadoresZonas = new JsonArray();
        Map<String, JsonObject> animalesPorId = new LinkedHashMap<>();
        List<Animal> animalesVisibles = new ArrayList<>();
        JsonArray tazas = new JsonArray();
        JsonArray banderas = new JsonArray();
        for (Zona zona : visibles) {
            identificadoresZonas.add(zona.consultaIdentificador());
            for (Animal animal : zona.animalesEnZona()) {
                if (animal.consultaVida()) {
                    animalesVisibles.add(animal);
                }
            }
            for (Taza taza : zona.tazasEnZona()) {
                if (!taza.consultaConsumida()) {
                    tazas.add(datosTaza(taza, zona));
                }
            }
            for (Bandera bandera : sesion.consultaBanderas()) {
                if (bandera.consultaZona() == zona) {
                    banderas.add(datosBandera(bandera, jugador));
                }
            }
        }
        animalesVisibles.sort(Comparator.comparing(Animal::consultaIdentificador));
        for (Animal animal : animalesVisibles) {
            animalesPorId.putIfAbsent(animal.consultaIdentificador(), datosAnimal(animal, manada));
        }
        JsonArray animales = new JsonArray();
        animalesPorId.values().forEach(animales::add);
        contenido.add(ConstantesProtocolo.CAMPO_ZONAS_VISIBLES, identificadoresZonas);
        contenido.add(ConstantesProtocolo.CAMPO_ANIMALES, animales);
        contenido.add(ConstantesProtocolo.CAMPO_TAZAS, tazas);
        contenido.add(ConstantesProtocolo.CAMPO_BANDERAS, banderas);
        return contenido;
    }

    // Envía GAME_STARTED a cada jugador conectado, con su propia versión.
    public void envioInicio() {
        for (Jugador jugador : sesion.consultaJugadores()) {
            jugador.envioMensaje(mensajeInicio(jugador));
        }
    }

    // La zona donde empieza el jugador es la de su bandera.
    private Zona zonaInicial(Jugador jugador) {
        for (Bandera bandera : sesion.consultaBanderas()) {
            if (bandera.consultaPropietario() == jugador) {
                return bandera.consultaZona();
            }
        }
        throw new IllegalStateException("El jugador no tiene bandera");
    }

    private JsonObject datosAnimal(Animal animal, Manada manadaDelJugador) {
        JsonObject datos = new JsonObject();
        datos.addProperty(ConstantesProtocolo.CAMPO_ID, animal.consultaIdentificador());
        datos.addProperty(ConstantesProtocolo.CAMPO_ESPECIE, animal.consultaEspecie().consultaIdentificador());
        // Otro hilo puede estar moviendo al animal: se leen la energía y la posición juntas, dentro del mismo bloque.
        synchronized (animal) {
            datos.addProperty(ConstantesProtocolo.CAMPO_ENERGIA, animal.consultaEnergia());
            datos.addProperty(ConstantesProtocolo.CAMPO_X, animal.consultaPosicion().consultaPosicionX());
            datos.addProperty(ConstantesProtocolo.CAMPO_Y, animal.consultaPosicion().consultaPosicionY());
        }
        datos.addProperty(ConstantesProtocolo.CAMPO_ENERGIA_MAXIMA, animal.consultaManada().consultaEnergiaInicial());
        datos.addProperty(ConstantesProtocolo.CAMPO_ES_PROPIO, animal.consultaManada() == manadaDelJugador);
        datos.addProperty(ConstantesProtocolo.CAMPO_ES_BOT, animal.esBot());
        return datos;
    }

    private JsonObject datosTaza(Taza taza, Zona zona) {
        JsonObject datos = new JsonObject();
        datos.addProperty(ConstantesProtocolo.CAMPO_ID, taza.consultaIdentificador());
        datos.addProperty(ConstantesProtocolo.CAMPO_X, taza.consultaPosicion().consultaPosicionX());
        datos.addProperty(ConstantesProtocolo.CAMPO_Y, taza.consultaPosicion().consultaPosicionY());
        datos.addProperty(ConstantesProtocolo.CAMPO_ZONA, zona.consultaIdentificador());
        return datos;
    }

    private JsonObject datosBandera(Bandera bandera, Jugador jugador) {
        JsonObject datos = new JsonObject();
        datos.addProperty(ConstantesProtocolo.CAMPO_X, bandera.consultaPosicion().consultaPosicionX());
        datos.addProperty(ConstantesProtocolo.CAMPO_Y, bandera.consultaPosicion().consultaPosicionY());
        datos.addProperty(ConstantesProtocolo.CAMPO_ZONA, bandera.consultaZona().consultaIdentificador());
        datos.addProperty(ConstantesProtocolo.CAMPO_ES_PROPIO, bandera.consultaPropietario() == jugador);
        return datos;
    }

    public void actualizacionZona(Zona zona) {
    }

    public void actualizacionMovimiento(Animal animal) {
    }

    public void actualizacionCombate() {
    }

    public void actualizacionEstado() {
    }
}
