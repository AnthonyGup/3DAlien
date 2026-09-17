package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Type;

public class Symbol {
    public enum Kind { VARIABLE, FUNCION, ESTRUCTURA, CLASE, METODO, CONSTRUCTOR }

    private final String name;
    private final Type type;
    private final Kind kind;
    private final boolean isArray;
    private final boolean isParameter;
    private final boolean isField;
    private final int size;

    public Symbol(String name, Type type, Kind kind, boolean isArray, boolean isParameter, boolean isField, int size) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.isArray = isArray;
        this.isParameter = isParameter;
        this.isField = isField;
        this.size = size;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public Kind getKind() { return kind; }
    public boolean isArray() { return isArray; }
    public boolean isParameter() { return isParameter; }
    public boolean isField() { return isField; }
    public int getSize() { return size; }
}