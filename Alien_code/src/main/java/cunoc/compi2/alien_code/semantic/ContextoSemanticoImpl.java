package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.errors.ErrorType;

import java.util.Deque;
import java.util.LinkedList;

public class ContextoSemanticoImpl implements ContextoSemantico {
    private final SymbolTable symbolTable;
    private final ErrorListener errorListener;
    private int loopDepth;
    private int switchDepth;
    private final Deque<Type> pilaRetornos = new LinkedList<>();
    private final Deque<Scope> pilaClases = new LinkedList<>();
    private final Deque<Symbol> pilaClaseSymbol = new LinkedList<>();
    private int rutinaDepth = 0;
    private final Deque<Boolean> pilaConstructor = new LinkedList<>();
    private String archivoActual;

    public ContextoSemanticoImpl(SymbolTable symbolTable, ErrorListener errorListener) {
        this.symbolTable = symbolTable;
        this.errorListener = errorListener;
    }

    @Override
    public Type evaluar(Node nodo) {
        return nodo.analizar(this);
    }

    @Override
    public void entrarAmbito() {
        symbolTable.enterScope();
    }

    @Override
    public void salirAmbito() {
        symbolTable.exitScope();
    }

    @Override
    public boolean definir(Symbol simbolo) {
        if (symbolTable.isDefinedInCurrentScope(simbolo.getName())) {
            return false;
        }
        symbolTable.define(simbolo);
        return true;
    }

    @Override
    public Symbol resolver(String nombre) {
        return symbolTable.resolve(nombre);
    }

    @Override
    public void registrarError(int linea, int columna, String mensaje) {
        errorListener.addError(ErrorType.SEMANTICO, mensaje, linea, columna, archivoActual);
    }

    @Override
    public void entrarCiclo() {
        loopDepth++;
    }

    @Override
    public void salirCiclo() {
        loopDepth--;
    }

    @Override
    public boolean enCiclo() {
        return loopDepth > 0;
    }

    @Override
    public Scope ambitoActual() {
        return symbolTable.ambitoActual();
    }

    @Override
    public void pushTipoRetorno(Type tipo) {
        pilaRetornos.push(tipo);
    }

    @Override
    public void popTipoRetorno() {
        if (!pilaRetornos.isEmpty()) {
            pilaRetornos.pop();
        }
    }

    @Override
    public Type tipoRetornoActual() {
        return pilaRetornos.isEmpty() ? null : pilaRetornos.peek();
    }

    @Override
    public void entrarSwitch() {
        switchDepth++;
    }

    @Override
    public void salirSwitch() {
        switchDepth--;
    }

    @Override
    public boolean enSwitch() {
        return switchDepth > 0;
    }

    @Override
    public void entrarAmbitoDentroDe(Scope padre) {
        symbolTable.enterScopeWithin(padre);
    }

    @Override
    public void pushClaseActual(Scope miembros) {
        pilaClases.push(miembros);
    }

    @Override
    public void popClaseActual() {
        if (!pilaClases.isEmpty()) {
            pilaClases.pop();
        }
    }

    @Override
    public Scope ambitoDeClaseActual() {
        return pilaClases.isEmpty() ? null : pilaClases.peek();
    }

    @Override
    public void entrarClase(Symbol clase) {
        pilaClaseSymbol.push(clase);
    }

    @Override
    public void salirClase() {
        if (!pilaClaseSymbol.isEmpty()) {
            pilaClaseSymbol.pop();
        }
    }

    @Override
    public Symbol claseActual() {
        return pilaClaseSymbol.isEmpty() ? null : pilaClaseSymbol.peek();
    }

    @Override
    public void entrarRutina(boolean constructor) {
        rutinaDepth++;
        pilaConstructor.push(constructor);
    }

    @Override
    public void salirRutina() {
        if (rutinaDepth > 0) rutinaDepth--;
        if (!pilaConstructor.isEmpty()) pilaConstructor.pop();
    }

    @Override
    public boolean enRutina() {
        return rutinaDepth > 0;
    }

    @Override
    public boolean enConstructor() {
        return !pilaConstructor.isEmpty() && pilaConstructor.peek();
    }

    @Override
    public void ingresarPrograma(String archivo) {
        this.archivoActual = archivo;
    }
}
