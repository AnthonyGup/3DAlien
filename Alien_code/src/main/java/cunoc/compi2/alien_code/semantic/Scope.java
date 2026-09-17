package cunoc.compi2.alien_code.semantic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scope {
    private final Scope parent;
    private final Map<String, Symbol> symbols;
    private final List<Symbol> orden;

    public Scope(Scope parent) {
        this.parent = parent;
        this.symbols = new LinkedHashMap<>();
        this.orden = new ArrayList<>();
    }

    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
        orden.add(symbol);
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
        return orden;
    }
}
