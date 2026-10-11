# JungleAttack

### jungleattack.servidor.aplicacion

**AplicacionServidor**
- Métodos: `main(String[] args)`

**ConfiguracionServidor**
- Atributos estáticos: `PUERTO: int`, `MAX_PARTIDAS: int`

### jungleattack.servidor.red

**ServidorSockets**
- Atributos: `puerto: int`, `activo: boolean`
- Métodos: `inicio()`, `cierre()`

**ManejadorCliente**
- Atributos: `socket: Socket`, `contexto: ContextoCliente`, `emisor: EmisorMensajes`, `receptor: ReceptorMensajes`
- Métodos: `recepcionMensajes()`, `envioMensaje()`, `cierre()`

### jungleattack.servidor.protocolo

**Mensaje**
- Atributos: `tipo: String`, `contenido: String`

**ConversorMensajes**
- Métodos: `conversionDesdeJson()`, `conversionAJson()`

**ProcesadorMensajes**
- Atributos: `comandos: Map<String, ComandoCliente>`, `contexto: ContextoCliente`
- Métodos: `procesamientoMensaje()`

### jungleattack.servidor.comandos

**ComandoCliente** — interfaz
- Métodos: `ejecucion()`

**ContextoCliente**
- Atributos: `jugador: Jugador`, `sesion: SesionPartida`, `conexion: ManejadorCliente`
- Métodos: `actualizacionContexto()`, `envioMensaje()`

**ComandoCrearPartida**
- Métodos: `ejecucion()`

**ComandoUnirsePartida**
- Métodos: `ejecucion()`

**ComandoSeleccionarEspecie**
- Métodos: `ejecucion()`

**ComandoMoverAnimales**
- Métodos: `ejecucion()`

**ComandoAtacar**
- Métodos: `ejecucion()`

**ComandoConsultarZona**
- Métodos: `ejecucion()`

**ComandoDesconexion**
- Métodos: `ejecucion()`

### jungleattack.servidor.partidas

**GestorPartidas**
- Atributos: `partidas: Map<String, SesionPartida>`
- Métodos: `creacionPartida()`, `unionPartida()`, `eliminacionPartida()`, `partidaPorCodigo()`

**SesionPartida**
- Atributos: `codigo: String`, `jugadores: List<Jugador>`, `estado: EstadoPartida`, `mapa: Mapa`, `manadas: List<Manada>`, `tazas: List<Taza>`, `bandera: Bandera`, `resumen: ResumenPartida`, `motorJuego: MotorJuego`
- Métodos: `inicio()`, `actualizacion()`, `finalizacion()`, `jugadorPorConexion()`

**Jugador**
- Atributos: `apodo: String`, `especie: Especie`, `manada: Manada`, `conectado: boolean`
- Métodos: `seleccionEspecie()`, `desconexion()`

**EstadoPartida** — enum

**ResumenPartida**
- Atributos: `botsEliminados: int`, `animalesEliminados: int`, `tazasConsumidas: int`, `duracion: double`

### jungleattack.servidor.modelo

**Animal**
- Atributos: `energia: int`, `vivo: boolean`, `posicion: Punto`, `especie: Especie`
- Métodos: `movimiento()`, `recepcionDanio()`, `muerte()`

**Manada** — clase abstracta
- Atributos: `animales: List<Animal>`, `danio: int`, `energiaInicial: int`
- Métodos: `movimientoManada()`, `danioManada()`, `animalesVivos()`

**ManadaJugador** — hereda de `Manada`
- Métodos: `ataque()`, `consumoTaza()`, `recuperacionEnergia()`

**ManadaBots** — hereda de `Manada`
- Métodos: `ataqueAutomatico()`

**Especie** — enum

**Taza**
- Atributos: `posicion: Punto`, `consumida: boolean`
- Métodos: `consumo()`

**Bandera**
- Atributos: `posicion: Punto`
- Métodos: `deteccionContacto()`

### jungleattack.servidor.mapa

**Punto**
- Atributos: `posicionX: int`, `posicionY: int`

**Zona**
- Atributos: `identificador: int`, `posicion: Punto`, `conexiones: List<Camino>`

**Camino**
- Atributos: `inicio: Zona`, `destino: Zona`, `longitud: double`

**Mapa**
- Atributos: `zonas: List<Zona>`, `caminos: List<Camino>`
- Métodos: `generacionMapa()`, `zonaPorIdentificador()`, `zonasConectadas()`

**BuscadorRutas**
- Métodos: `busquedaRuta()`

### jungleattack.servidor.reglas

**MotorJuego**
- Atributos: `sesion: SesionPartida`
- Métodos: `actualizacionJuego()`, `comprobacionVictoria()`

**ReglasMovimiento**
- Métodos: `calculoRuta()`, `actualizacionPosicion()`

**ReglasCombate**
- Métodos: `calculoDanio()`, `aplicacionDanio()`, `comprobacionAlcance()`

**ReglasObjetos**
- Métodos: `colocacionTazas()`, `consumoTaza()`, `recuperacionPorEliminaciones()`

**ReglasVictoria**
- Métodos: `comprobacionVictoria()`, `creacionResumen()`

### jungleattack.servidor.constantes

**ConstantesJuego**
- Atributos estáticos: `ENERGIA_MANADA_JUGADOR: int`, `ENERGIA_ANIMAL: int`, `CANTIDAD_ANIMALES_JUGADOR: int`, `CANTIDAD_ANIMALES_BOTS: int`, `DANIO_JUGADOR: int`, `DANIO_BOTS: int`, `CANTIDAD_TAZAS: int`, `ATTACK_COOLDOWN_MS: int`, `ENCOUNTER_RADIUS: double`

## Herencia, interfaces y polimorfismo

- `Manada` es la clase abstracta de `ManadaJugador` y `ManadaBots`.
- `ComandoCliente` es implementada por los comandos concretos.
- `ProcesadorMensajes` ejecuta los comandos mediante la interfaz `ComandoCliente`, utilizando polimorfismo.

## Elementos estáticos

- `ConfiguracionServidor` contiene las constantes de configuración del servidor.
- `ConstantesJuego` contiene las constantes de las reglas del juego.

## Patrones de diseño

**Command**

- Interfaz: `ComandoCliente`.
- Comandos concretos: `ComandoCrearPartida`, `ComandoUnirsePartida`, `ComandoSeleccionarEspecie`, `ComandoMoverAnimales`, `ComandoAtacar`, `ComandoConsultarZona` y `ComandoDesconexion`.
- Invocador: `ProcesadorMensajes`.
- Cada comando encapsula una operación solicitada por un cliente.

## Diagrama de clases

```mermaid
classDiagram
    class AplicacionServidor {
        +main(String[] args)
    }

    class ConfiguracionServidor {
        +PUERTO: int$
        +MAX_PARTIDAS: int$
    }

    class ServidorSockets {
        -puerto: int
        -activo: boolean
        +inicio()
        +cierre()
    }

    class ManejadorCliente {
        -socket: Socket
        -contexto: ContextoCliente
        -emisor: EmisorMensajes
        -receptor: ReceptorMensajes
        +recepcionMensajes()
        +envioMensaje()
        +cierre()
    }

    AplicacionServidor --> ServidorSockets
    ServidorSockets ..> ConfiguracionServidor
    ServidorSockets --> ManejadorCliente

    class Mensaje {
        +tipo: String
        +contenido: String
    }

    class ConversorMensajes {
        +conversionDesdeJson()
        +conversionAJson()
    }

    class ProcesadorMensajes {
        -comandos: Map~String, ComandoCliente~
        -contexto: ContextoCliente
        +procesamientoMensaje()
    }

    ManejadorCliente --> ProcesadorMensajes
    ManejadorCliente --> Mensaje
    ConversorMensajes ..> Mensaje
    ProcesadorMensajes --> ComandoCliente

    class ComandoCliente {
        <<interface>>
        +ejecucion()
    }

    class ContextoCliente {
        -jugador: Jugador
        -sesion: SesionPartida
        -conexion: ManejadorCliente
        +actualizacionContexto()
        +envioMensaje()
    }

    class ComandoCrearPartida {
        +ejecucion()
    }

    class ComandoUnirsePartida {
        +ejecucion()
    }

    class ComandoSeleccionarEspecie {
        +ejecucion()
    }

    class ComandoMoverAnimales {
        +ejecucion()
    }

    class ComandoAtacar {
        +ejecucion()
    }

    class ComandoConsultarZona {
        +ejecucion()
    }

    class ComandoDesconexion {
        +ejecucion()
    }

    ComandoCliente <|.. ComandoCrearPartida
    ComandoCliente <|.. ComandoUnirsePartida
    ComandoCliente <|.. ComandoSeleccionarEspecie
    ComandoCliente <|.. ComandoMoverAnimales
    ComandoCliente <|.. ComandoAtacar
    ComandoCliente <|.. ComandoConsultarZona
    ComandoCliente <|.. ComandoDesconexion

    ProcesadorMensajes --> ContextoCliente

    class GestorPartidas {
        -partidas: Map~String, SesionPartida~
        +creacionPartida()
        +unionPartida()
        +eliminacionPartida()
        +partidaPorCodigo()
    }

    class SesionPartida {
        -codigo: String
        -jugadores: List~Jugador~
        -estado: EstadoPartida
        -mapa: Mapa
        -manadas: List~Manada~
        -tazas: List~Taza~
        -bandera: Bandera
        -resumen: ResumenPartida
        -motorJuego: MotorJuego
        +inicio()
        +actualizacion()
        +finalizacion()
        +jugadorPorConexion()
    }

    class Jugador {
        -apodo: String
        -especie: Especie
        -manada: Manada
        -conectado: boolean
        +seleccionEspecie()
        +desconexion()
    }

    class EstadoPartida {
        <<enumeration>>
    }

    class ResumenPartida {
        -botsEliminados: int
        -animalesEliminados: int
        -tazasConsumidas: int
        -duracion: double
    }

    GestorPartidas --> SesionPartida
    SesionPartida --> Jugador
    SesionPartida --> EstadoPartida
    SesionPartida --> ResumenPartida

    class Animal {
        -energia: int
        -vivo: boolean
        -posicion: Punto
        -especie: Especie
        +movimiento()
        +recepcionDanio()
        +muerte()
    }

    class Manada {
        <<abstract>>
        -animales: List~Animal~
        -danio: int
        -energiaInicial: int
        +movimientoManada()
        +danioManada()
        +animalesVivos()
    }

    class ManadaJugador {
        +ataque()
        +consumoTaza()
        +recuperacionEnergia()
    }

    class ManadaBots {
        +ataqueAutomatico()
    }

    class Especie {
        <<enumeration>>
    }

    Manada <|-- ManadaJugador
    Manada <|-- ManadaBots
    Manada o-- Animal
    Animal --> Especie
    Jugador --> Especie
    Jugador --> Manada

    class Taza {
        -posicion: Punto
        -consumida: boolean
        +consumo()
    }

    class Bandera {
        -posicion: Punto
        +deteccionContacto()
    }

    class Punto {
        -posicionX: int
        -posicionY: int
    }

    class Zona {
        -identificador: int
        -posicion: Punto
        -conexiones: List~Camino~
    }

    class Camino {
        -inicio: Zona
        -destino: Zona
        -longitud: double
    }

    class Mapa {
        -zonas: List~Zona~
        -caminos: List~Camino~
        +generacionMapa()
        +zonaPorIdentificador()
        +zonasConectadas()
    }

    class BuscadorRutas {
        +busquedaRuta()
    }

    Mapa o-- Zona
    Mapa o-- Camino
    Zona --> Punto
    Zona --> Camino
    Camino --> Zona
    Animal --> Punto
    Taza --> Punto
    Bandera --> Punto
    BuscadorRutas --> Mapa

    SesionPartida --> Mapa
    SesionPartida --> Taza
    SesionPartida --> Bandera
    SesionPartida --> Manada
    SesionPartida --> MotorJuego

    class MotorJuego {
        -sesion: SesionPartida
        +actualizacionJuego()
        +comprobacionVictoria()
    }

    class ReglasMovimiento {
        +calculoRuta()
        +actualizacionPosicion()
    }

    class ReglasCombate {
        +calculoDanio()
        +aplicacionDanio()
        +comprobacionAlcance()
    }

    class ReglasObjetos {
        +colocacionTazas()
        +consumoTaza()
        +recuperacionPorEliminaciones()
    }

    class ReglasVictoria {
        +comprobacionVictoria()
        +creacionResumen()
    }

    MotorJuego --> SesionPartida
    MotorJuego --> ReglasMovimiento
    MotorJuego --> ReglasCombate
    MotorJuego --> ReglasObjetos
    MotorJuego --> ReglasVictoria

    class ConstantesJuego {
        +ENERGIA_MANADA_JUGADOR: int$
        +ENERGIA_ANIMAL: int$
        +CANTIDAD_ANIMALES_JUGADOR: int$
        +CANTIDAD_ANIMALES_BOTS: int$
        +DANIO_JUGADOR: int$
        +DANIO_BOTS: int$
        +CANTIDAD_TAZAS: int$
        +ATTACK_COOLDOWN_MS: int$
        +ENCOUNTER_RADIUS: double$
    }

    ServidorSockets --> ManejadorCliente
    ManejadorCliente --> ContextoCliente
    ContextoCliente --> Jugador
    ContextoCliente --> SesionPartida
```
