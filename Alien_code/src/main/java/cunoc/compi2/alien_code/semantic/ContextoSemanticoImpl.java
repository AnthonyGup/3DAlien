package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.errors.ErrorType;

public class ContextoSemanticoImpl implements ContextoSemantico {
    private final SymbolTable symbolTable;
    private final ErrorListener errorListener;
    private int loopDepth;
    private int switchDepth;
    private final Pila<Type> pilaRetornos = new Pila<>();
    private final Pila<Scope> pilaClases = new Pila<>();
    private final Pila<Symbol> pilaClaseSymbol = new Pila<>();
    private int rutinaDepth = 0;
    private final Pila<Boolean> pilaConstructor = new Pila<>();
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
        pilaRetornos.empujar(tipo);
    }

    @Override
    public void popTipoRetorno() {
        pilaRetornos.sacar();
    }

    @Override
    public Type tipoRetornoActual() {
        return pilaRetornos.tope();
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
        pilaClases.empujar(miembros);
    }

    @Override
    public void popClaseActual() {
        pilaClases.sacar();
    }

    @Override
    public Scope ambitoDeClaseActual() {
        return pilaClases.tope();
    }

    @Override
    public void entrarClase(Symbol clase) {
        pilaClaseSymbol.empujar(clase);
    }

    @Override
    public void salirClase() {
        pilaClaseSymbol.sacar();
    }

    @Override
    public Symbol claseActual() {
        return pilaClaseSymbol.tope();
    }

    @Override
    public void entrarRutina(boolean constructor) {
        rutinaDepth++;
        pilaConstructor.empujar(constructor);
    }

    @Override
    public void salirRutina() {
        if (rutinaDepth > 0) rutinaDepth--;
        pilaConstructor.sacar();
    }

    @Override
    public boolean enRutina() {
        return rutinaDepth > 0;
    }

    @Override
    public boolean enConstructor() {
        return Boolean.TRUE.equals(pilaConstructor.tope());
    }

    @Override
    public void ingresarPrograma(String archivo) {
        this.archivoActual = archivo;
    }
}
