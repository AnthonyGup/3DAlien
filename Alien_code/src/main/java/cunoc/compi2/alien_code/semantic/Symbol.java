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
    private String tipoNombre; // NUEVO: nombre real cuando type == STRUCT/CLASS, null si no aplica
    private Scope miembros; // NUEVO: ámbito con los campos/métodos, solo para ESTRUCTURA/CLASE

   

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

    public String getTipoNombre() { return tipoNombre; }
    public void setTipoNombre(String tipoNombre) { this.tipoNombre = tipoNombre; }

    public Scope getMiembros() { return miembros; }
    public void setMiembros(Scope miembros) { this.miembros = miembros; }

}