package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.errors.ErrorListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class IntermediateCodeGenerator implements CodigoContexto {
    private final List<Cuarteta> cuartetas;
    private final ErrorListener errorListener;
    private final Deque<String[]> ciclos;
    private int tempCounter;
    private int labelCounter;

    public IntermediateCodeGenerator(ErrorListener errorListener) {
        this.cuartetas = new ArrayList<>();
        this.errorListener = errorListener;
        this.ciclos = new ArrayDeque<>();
        this.tempCounter = 0;
        this.labelCounter = 0;
    }

    public List<Cuarteta> getCuartetas() {
        return cuartetas;
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    @Override
    public String nuevoTemporal() {
        return "t" + (tempCounter++);
    }

    @Override
    public String nuevaEtiqueta() {
        return "L" + (labelCounter++);
    }

    @Override
    public void emitir(String operador, String operando1, String operando2, String resultado) {
        cuartetas.add(new Cuarteta(operador, operando1, operando2, resultado));
    }

    @Override
    public void empujarCiclo(String etiquetaContinuar, String etiquetaSalida) {
        ciclos.push(new String[]{etiquetaContinuar, etiquetaSalida});
    }

    @Override
    public void popCiclo() {
        ciclos.pop();
    }

    @Override
    public String etiquetaContinuarActual() {
        return ciclos.isEmpty() ? null : ciclos.peek()[0];
    }

    @Override
    public String etiquetaSalidaActual() {
        return ciclos.isEmpty() ? null : ciclos.peek()[1];
    }
}