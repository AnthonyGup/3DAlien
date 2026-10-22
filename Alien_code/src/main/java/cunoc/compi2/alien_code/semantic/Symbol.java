package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.Visibilidad;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
    public enum Kind { VARIABLE, FUNCION, ESTRUCTURA, CLASE, METODO, CONSTRUCTOR }

    private final String name;
    private final Type type;
    private final Kind kind;
    private final boolean isArray;
    private final boolean isParameter;
    private final boolean isField;
    private final int size;
    private boolean nativa;
    private int linea;
    private int columna;
    private int dimensiones;
    private String tipoNombre; // NUEVO: nombre real cuando type == STRUCT/CLASS, null si no aplica
    private Scope miembros; // NUEVO: ámbito con los campos/métodos, solo para ESTRUCTURA/CLASE
    private List<Type> tiposParametros = new ArrayList<>();
    private List<List<Type>> firmas = new ArrayList<>();
    private List<List<Type>> firmasConstructores = new ArrayList<>();
    private int offset = -1;
    private boolean offsetGlobal = false;
    private Visibilidad visibilidad = Visibilidad.DEFAULT;
    private String claseDuena;
    private String nombreSuperclase;
    private Symbol superclaseResuelta;
    private boolean tieneOverride = false;
    private final List<Symbol> constructores = new ArrayList<>();

   

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
    public boolean isField() { return isField; }
    public boolean isParameter() { return isParameter; }
    public int getSize() { return size; }

    public boolean isNativa() { return nativa; }
    public void setNativa(boolean nativa) { this.nativa = nativa; }

    public int getLinea() { return linea; }
    public void setLinea(int linea) { this.linea = linea; }

    public void setColumna(int columna) { this.columna = columna; }

    public int getDimensiones() { return dimensiones; }
    public void setDimensiones(int dimensiones) { this.dimensiones = dimensiones; }

    public String getTipoNombre() { return tipoNombre; }
    public void setTipoNombre(String tipoNombre) { this.tipoNombre = tipoNombre; }

    public Scope getMiembros() { return miembros; }
    public void setMiembros(Scope miembros) { this.miembros = miembros; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public boolean isOffsetGlobal() { return offsetGlobal; }
    public void setOffsetGlobal(boolean offsetGlobal) { this.offsetGlobal = offsetGlobal; }

    public List<Type> getTiposParametros() { return tiposParametros; }

    public List<List<Type>> getFirmas() { return firmas; }

    public List<List<Type>> getFirmasConstructores() { return firmasConstructores; }

    public Visibilidad getVisibilidad() { return visibilidad; }
    public void setVisibilidad(Visibilidad visibilidad) { this.visibilidad = visibilidad; }

    public String getClaseDuena() { return claseDuena; }
    public void setClaseDuena(String claseDuena) { this.claseDuena = claseDuena; }

    public String getNombreSuperclase() { return nombreSuperclase; }
    public void setNombreSuperclase(String nombreSuperclase) { this.nombreSuperclase = nombreSuperclase; }

    public Symbol getSuperclaseResuelta() { return superclaseResuelta; }
    public void setSuperclaseResuelta(Symbol superclaseResuelta) { this.superclaseResuelta = superclaseResuelta; }

    public boolean tieneOverride() { return tieneOverride; }
    public void setTieneOverride(boolean tieneOverride) { this.tieneOverride = tieneOverride; }

    public List<Symbol> getConstructores() { return constructores; }
    public void agregarConstructor(Symbol ctor) { constructores.add(ctor); }

    public static Symbol variable(String nombre, Type tipoDeclarado, Type tipoElemento,
            String tipoNombre, int dimensiones, boolean isParameter, boolean isField,
            int line, int column) {
        Type tipoSimbolo = tipoDeclarado == Type.ARRAY && tipoElemento != null
                ? tipoElemento : tipoDeclarado;
        Symbol s = new Symbol(nombre, tipoSimbolo, Symbol.Kind.VARIABLE,
                tipoDeclarado == Type.ARRAY, isParameter, isField, 0);
        if (tipoDeclarado == Type.ARRAY) {
            s.setDimensiones(dimensiones);
        }
        s.setLinea(line);
        s.setColumna(column);
        if (tipoSimbolo == Type.STRUCT || tipoSimbolo == Type.CLASS) {
            s.setTipoNombre(tipoNombre);
        }
        return s;
    }

    public void agregarFirma(List<Type> tipos) {
        firmas.add(tipos);
    }

    public void agregarFirmaConstructor(List<Type> tipos) {
        firmasConstructores.add(tipos);
    }

    public boolean tieneFirmaCompatible(List<Type> argumentos) {
        return coincideAlguna(firmas, argumentos);
    }

    public boolean tieneConstructorCompatible(List<Type> argumentos) {
        return coincideAlguna(firmasConstructores, argumentos);
    }

    private boolean coincideAlguna(List<List<Type>> candidatas, List<Type> argumentos) {
        for (List<Type> firma : candidatas) {
            if (firma.size() != argumentos.size()) continue;
            boolean ok = true;
            for (int i = 0; i < firma.size(); i++) {
                if (!TypeCompat.esAsignable(firma.get(i), argumentos.get(i))) { ok = false; break; }
            }
            if (ok) return true;
        }
        return false;
    }
}
