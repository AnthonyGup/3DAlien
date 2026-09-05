package cunoc.compi2.alien_code.semantic;

import java.util.Stack;

public class SymbolTable {
    private final Stack<Scope> scopes;

    public SymbolTable() {
        this.scopes = new Stack<>();
        enterScope();
    }

    public void enterScope() {
        Scope parent = scopes.isEmpty() ? null : scopes.peek();
        scopes.push(new Scope(parent));
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    public void define(Symbol symbol) {
        scopes.peek().define(symbol);
    }

    public Symbol resolve(String name) {
        return scopes.peek().resolve(name);
    }
}
