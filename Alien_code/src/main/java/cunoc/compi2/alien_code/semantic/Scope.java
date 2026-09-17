package cunoc.compi2.alien_code.semantic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Symbol> symbols;

    public Scope(Scope parent) {
        this.parent = parent;
        this.symbols = new HashMap<>();
    }

    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    public boolean contains(String name) {
        return symbols.containsKey(name);
    }

    public Symbol resolve(String name) {
        Symbol s = symbols.get(name);
        if (s != null) return s;
        if (parent != null) return parent.resolve(name);
        return null;
    }

    public Scope getParent() {
        return parent;
    }

    public Collection<Symbol> getTodos() {
        return symbols.values();
    }
}
