package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

public interface CodigoContexto {
    int nuevoIndiceTemporal();

    String nuevaEtiqueta();

    void agregar(Cuarteta cuarteta);

    void empujarCiclo(String etiquetaContinuar, String etiquetaSalida);

    void popCiclo();

    String etiquetaContinuarActual();

    String etiquetaSalidaActual();

    void pushClassTranslation(String nombre);

    void popClassTranslation();

    String currentClassName();

    int nextMethodIndex(String methodName);

    int nextConstructorIndex();

    void registrarTipoTemporal(int temporal, String ctype);

    String tipoDeTemporal(int temporal);

    Symbol resolverSimbolo(String nombre);

    SymbolTable getSymbolTable();
}