package cunoc.compi2.alien_code.c3d;

import cunoc.compi2.alien_code.ast.Type;

public final class TiposC {
    private TiposC() {
    }

    public static String cType(Type t, String tipoNombre) {
        if (t == null) {
            return "void*";
        }
        switch (t) {
            case INT:
            case BOOL:
                return "int";
            case FLOAT:
                return "double";
            case CHAR:
                return "char";
            case STRING:
                return "char*";
            case STRUCT:
            case CLASS:
                return "struct " + (tipoNombre != null ? tipoNombre : "X") + " *";
            case VOID:
                return "void";
            default:
                return "void*";
        }
    }

    public static Type typeOfCType(String ctype) {
        if (ctype == null) {
            return null;
        }
        switch (ctype) {
            case "int":
                return Type.INT;
            case "double":
                return Type.FLOAT;
            case "char":
                return Type.CHAR;
            case "char*":
                return Type.STRING;
            default:
                if (ctype.startsWith("struct ")) {
                    return Type.STRUCT;
                }
                return ctype.endsWith("*") ? Type.ARRAY : Type.INT;
        }
    }

    public static boolean isComparison(String op) {
        if (op == null) {
            return false;
        }
        switch (op) {
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "&&":
            case "||":
                return true;
            default:
                return false;
        }
    }

    public static String binaryResultType(Type a, Type b, String op) {
        if (isComparison(op)) {
            return "int";
        }
        if (a == Type.STRING || b == Type.STRING) {
            return "char*";
        }
        if (a == Type.FLOAT || b == Type.FLOAT) {
            return "double";
        }
        return "int";
    }

    public static String elementTypeNew(String elemWord) {
        String w = elemWord == null ? "" : elemWord.toLowerCase();
        switch (w) {
            case "int":
            case "bool":
                return "int";
            case "double":
                return "double";
            case "char":
                return "char";
            case "string":
                return "char*";
            default:
                return "struct " + elemWord + " *";
        }
    }

    public static String mallocSize(String elemWord) {
        String w = elemWord == null ? "" : elemWord.toLowerCase();
        switch (w) {
            case "int":
            case "bool":
                return "sizeof(int)";
            case "double":
                return "sizeof(double)";
            case "char":
                return "sizeof(char)";
            case "string":
                return "sizeof(char*)";
            default:
                return "sizeof(struct " + elemWord + " *)";
        }
    }

    public static String mallocSizePtr(String elemWord) {
        String w = elemWord == null ? "" : elemWord.toLowerCase();
        switch (w) {
            case "int":
            case "bool":
                return "sizeof(int*)";
            case "double":
                return "sizeof(double*)";
            case "char":
                return "sizeof(char*)";
            case "string":
                return "sizeof(char**)";
            default:
                return "sizeof(struct " + elemWord + " *)";
        }
    }
}