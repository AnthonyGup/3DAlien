package cunoc.compi2.alien_code.ast;
import cunoc.compi2.alien_code.ast.Type;

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
            case "int" -> INT;
            case "double" -> FLOAT;
            case "string" -> STRING;
            case "boolean" -> BOOL;
            case "char" -> CHAR;
            case "void" -> VOID;
            default -> null;
        };
    }

    public static Type fromPigLatin(String name) {
        return switch (name.toLowerCase()) {
            case "numerus" -> INT;
            case "decimalis" -> FLOAT;
            case "textum" -> STRING;
            case "littera" -> CHAR;
            case "bool" -> BOOL;
            default -> null;
        };
    }
}
