package cunoc.compi2.alien_code.ast;

public interface CodigoContexto {
    String nuevoTemporal();

    String nuevaEtiqueta();

    void emitir(String operador, String operando1, String operando2, String resultado);

    void empujarCiclo(String etiquetaContinuar, String etiquetaSalida);

    void popCiclo();

    String etiquetaContinuarActual();

    String etiquetaSalidaActual();
}