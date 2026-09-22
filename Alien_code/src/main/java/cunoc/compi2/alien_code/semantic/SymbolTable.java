package cunoc.compi2.alien_code.semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SymbolTable {
    private final Stack<Scope> scopes;
    private final List<Symbol> registro;

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.registro = new ArrayList<>();
        enterScope();
    }

    public void enterScope() {
        Scope parent = scopes.isEmpty() ? null : scopes.peek();
        scopes.push(new Scope(parent));
    }

    public void enterScopeWithin(Scope parent) {
        scopes.push(new Scope(parent));
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    public boolean isDefinedInCurrentScope(String name) {
        return scopes.peek().contains(name);
    }

    public void define(Symbol symbol) {
        scopes.peek().define(symbol);
        registro.add(symbol);
    }

    public Symbol resolve(String name) {
        return scopes.peek().resolve(name);
    }

    public Scope ambitoActual() {
        return scopes.peek();
    }

    public List<Symbol> listarSimbolos() {
        List<Symbol> simbolos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Symbol simbolo : registro) {
            if (vistos.add(simbolo.getName())) {
                simbolos.add(simbolo);
            }
        }
        return simbolos;
    }
}
