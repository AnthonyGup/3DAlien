package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntermediateCodeGenerator implements CodigoContexto {
    private final List<Cuarteta> instrucciones;
    private final Map<Integer, String> tiposTemporales;
    private final Deque<String[]> ciclos;
    private final SymbolTable tabla;
    private int tempCounter;
    private int labelCounter;
    private final Deque<InfoClase> pilaClases = new ArrayDeque<>();
    private Map<String, Symbol> simbolosPlano;

    public IntermediateCodeGenerator(ErrorListener errorListener) {
        this(errorListener, null);
    }

    public IntermediateCodeGenerator(ErrorListener errorListener, SymbolTable tabla) {
        this.instrucciones = new ArrayList<>();
        this.tiposTemporales = new HashMap<>();
        this.ciclos = new ArrayDeque<>();
        this.tabla = tabla;
        this.tempCounter = 0;
        this.labelCounter = 0;
    }

    public List<Cuarteta> getInstrucciones() {
        return instrucciones;
    }

    public Map<Integer, String> getTiposTemporales() {
        return tiposTemporales;
    }

    @Override
    public int nuevoIndiceTemporal() {
        return tempCounter++;
    }

    @Override
    public String nuevaEtiqueta() {
        return "L" + (labelCounter++);
    }

    @Override
    public void agregar(Cuarteta cuarteta) {
        instrucciones.add(cuarteta);
    }

    @Override
    public void registrarTipoTemporal(int temporal, String ctype) {
        if (ctype != null && !ctype.equals("void")) {
            tiposTemporales.put(temporal, ctype);
        }
    }

    @Override
    public String tipoDeTemporal(int temporal) {
        return tiposTemporales.get(temporal);
    }

    @Override
    public Symbol resolverSimbolo(String nombre) {
        if (nombre == null) {
            return null;
        }
        if (simbolosPlano == null && tabla != null) {
            simbolosPlano = new HashMap<>();
            for (Symbol s : tabla.listarSimbolos()) {
                simbolosPlano.putIfAbsent(s.getName(), s);
            }
        }
        return simbolosPlano == null ? null : simbolosPlano.get(nombre);
    }

    @Override
    public SymbolTable getSymbolTable() {
        return tabla;
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

    @Override
    public void pushClassTranslation(String nombre) {
        pilaClases.push(new InfoClase(nombre));
    }

    @Override
    public void popClassTranslation() {
        if (!pilaClases.isEmpty()) {
            pilaClases.pop();
        }
    }

    @Override
    public String currentClassName() {
        return pilaClases.isEmpty() ? null : pilaClases.peek().nombre;
    }

    @Override
    public int nextMethodIndex(String methodName) {
        return pilaClases.isEmpty() ? 0 : pilaClases.peek().nextMethodIndex(methodName);
    }

    @Override
    public int nextConstructorIndex() {
        return pilaClases.isEmpty() ? 0 : pilaClases.peek().nextConstructorIndex();
    }

    public int overloadIndexByArgCount(List<List<Type>> firmas, int argCount) {
        for (int i = 0; i < firmas.size(); i++) {
            if (firmas.get(i).size() == argCount) {
                return i;
            }
        }
        return 0;
    }

    private static final class InfoClase {
        final String nombre;
        final Map<String, Integer> indicesMetodos = new HashMap<>();
        int indicesConstructores;

        InfoClase(String nombre) {
            this.nombre = nombre;
        }

        int nextMethodIndex(String methodName) {
            int actual = indicesMetodos.getOrDefault(methodName, 0);
            indicesMetodos.put(methodName, actual + 1);
            return actual;
        }

        int nextConstructorIndex() {
            return indicesConstructores++;
        }
    }
}