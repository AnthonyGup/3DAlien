package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.semantic.Scope;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntermediateCodeGenerator implements CodigoContexto {
    private final List<Cuarteta> cuartetas;
    private final ErrorListener errorListener;
    private final Deque<String[]> ciclos;
    private int tempCounter;
    private int labelCounter;
    private SymbolTable symbolTable;
    private Scope globalScope;
    private final Map<String, Type> temporalTypes = new HashMap<>();
    private final Map<String, Boolean> temporalIsArray = new HashMap<>();
    private final Map<String, Integer> temporalDimensiones = new HashMap<>();
    private final Map<String, String> temporalTipoNombres = new HashMap<>();
    private final Deque<Scope> scopesTraduccion = new ArrayDeque<>();
    private final Deque<InfoClase> pilaClases = new ArrayDeque<>();

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

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.globalScope = symbolTable.ambitoActual();
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

    public void recordTemporalType(String name, Type type) {
        recordTemporal(name, type, false, 0, null);
    }

    public void recordTemporal(String name, Type type, boolean isArray, int dimensiones, String tipoNombre) {
        temporalTypes.put(name, type);
        temporalIsArray.put(name, isArray);
        temporalDimensiones.put(name, Math.max(0, dimensiones));
        if (type == Type.STRUCT || type == Type.CLASS) {
            temporalTipoNombres.put(name, tipoNombre);
        }
    }

    public Type getTemporalType(String name) {
        return temporalTypes.get(name);
    }

    public ValueInfo describeValue(String name) {
        if (name == null) {
            return null;
        }
        if (temporalTypes.containsKey(name)) {
            return new ValueInfo(temporalTypes.get(name),
                    temporalIsArray.getOrDefault(name, false),
                    temporalDimensiones.getOrDefault(name, 0),
                    temporalTipoNombres.get(name));
        }
        Symbol simbolo = resolveForTranslation(name);
        if (simbolo != null) {
            return new ValueInfo(simbolo.getType(), simbolo.isArray(),
                    simbolo.getDimensiones(), simbolo.getTipoNombre());
        }
        return null;
    }

    public void entrarAmbitoTraduccion() {
        Scope parent = scopesTraduccion.isEmpty() ? globalScope : scopesTraduccion.peek();
        scopesTraduccion.push(new Scope(parent));
    }

    public void entrarAmbitoTraduccion(Scope parent) {
        scopesTraduccion.push(new Scope(parent));
    }

    public void salirAmbitoTraduccion() {
        if (!scopesTraduccion.isEmpty()) {
            scopesTraduccion.pop();
        }
    }

    public boolean enAmbitoTraduccion() {
        return !scopesTraduccion.isEmpty();
    }

    public void definirEnTraduccion(Symbol simbolo) {
        if (!scopesTraduccion.isEmpty()) {
            scopesTraduccion.peek().define(simbolo);
        }
    }

    public Symbol resolveForTranslation(String name) {
        if (scopesTraduccion.isEmpty()) {
            return symbolTable != null ? symbolTable.resolve(name) : null;
        }
        return scopesTraduccion.peek().resolve(name);
    }

    public void pushClassTranslation(String nombre, Scope miembros) {
        pilaClases.push(new InfoClase(nombre, miembros));
    }

    public void popClassTranslation() {
        if (!pilaClases.isEmpty()) {
            pilaClases.pop();
        }
    }

    public String currentClassName() {
        return pilaClases.isEmpty() ? null : pilaClases.peek().nombre;
    }

    public Scope currentClassMembers() {
        return pilaClases.isEmpty() ? null : pilaClases.peek().miembros;
    }

    public int nextMethodIndex(String methodName) {
        return pilaClases.isEmpty() ? 0 : pilaClases.peek().nextMethodIndex(methodName);
    }

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

    public static class ValueInfo {
        public final Type type;
        public final boolean isArray;
        public final int dimensiones;
        public final String tipoNombre;

        public ValueInfo(Type type, boolean isArray, int dimensiones, String tipoNombre) {
            this.type = type;
            this.isArray = isArray;
            this.dimensiones = dimensiones;
            this.tipoNombre = tipoNombre;
        }
    }

    private static final class InfoClase {
        final String nombre;
        final Scope miembros;
        final Map<String, Integer> indicesMetodos = new HashMap<>();
        int indicesConstructores;

        InfoClase(String nombre, Scope miembros) {
            this.nombre = nombre;
            this.miembros = miembros;
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