package jungleattack.servidor.comandos;

import jungleattack.servidor.protocolo.Mensaje;

public interface ComandoCliente {

    void ejecucion(Mensaje mensaje, ContextoCliente contexto);
}
