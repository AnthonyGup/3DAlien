package cunoc.compi2.alien_code.ast;

public enum Type {
    INT,
    FLOAT,
    STRING,
    BOOL,
    CHAR,
    VOID,
    STRUCT,
    CLASS,
    ARRAY;

    public static Type fromYLang(String name) {
        return switch (name.toLowerCase()) {
            case "entero" -> INT;
            case "decimal" -> FLOAT;
            case "texto" -> STRING;
            case "booleano" -> BOOL;
            case "caracter" -> CHAR;
            case "vacio" -> VOID;
            default -> null;
        };
    }

    public static Type fromZetariano(String name) {
        return switch (name.toLowerCase()) {
            case "numerus" -> INT;
            case "numerusdecimalis" -> FLOAT;
            case "verbum" -> STRING;
            case "verum" -> BOOL;
            case "littera" -> CHAR;
            case "vacuum" -> VOID;
            default -> null;
        };
    }

    public static Type fromPigLatin(String name) {
        return switch (name.toLowerCase()) {
            case "int" -> INT;
            case "float" -> FLOAT;
            case "string" -> STRING;
            case "bool" -> BOOL;
            case "char" -> CHAR;
            case "void" -> VOID;
            default -> null;
        };
    }
}
