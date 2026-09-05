package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Type;

public class Symbol {
    private final String name;
    private final Type type;
    private final boolean isArray;
    private final boolean isParameter;
    private final boolean isField;

    public Symbol(String name, Type type, boolean isArray, boolean isParameter, boolean isField) {
        this.name = name;
        this.type = type;
        this.isArray = isArray;
        this.isParameter = isParameter;
        this.isField = isField;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public boolean isArray() { return isArray; }
    public boolean isParameter() { return isParameter; }
    public boolean isField() { return isField; }
}
