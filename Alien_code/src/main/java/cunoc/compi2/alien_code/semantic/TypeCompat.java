package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Objects;

public final class TypeCompat {

    // true si el tipo es un numero (entero o flotante)
    public static boolean esNumerico(Type t) {
        return t == Type.INT || t == Type.FLOAT;
    }

    // true si un valor tipo origen se puede guardar en una variable tipo destino
    public static boolean esAsignable(Type destino, Type origen) {
        if (destino == null || origen == null) return true;
        if (destino == origen) return true;
        return destino == Type.FLOAT && origen == Type.INT;
    }

    // igual que arriba pero tambien revisa que el nombre coincida para structs/objetos
    public static boolean esAsignable(Type destino, String destinoNombre, Type origen, String origenNombre) {
        if (destino == null || origen == null) return true;
        if (destino == Type.STRUCT || destino == Type.CLASS) {
            return destino == origen && Objects.equals(destinoNombre, origenNombre);
        }
        return esAsignable(destino, origen);
    }

    // tipo que resulta de una operacion aritmetica (ej: int + float = float, string + x = string)
    public static Type tipoAritmetico(Type izq, Type der) {
        if (izq == null || der == null) return null;
        if (izq == Type.STRING || der == Type.STRING) return Type.STRING;
        if (izq == Type.FLOAT || der == Type.FLOAT) {
            return esNumerico(izq) && esNumerico(der) ? Type.FLOAT : null;
        }
        if (izq == Type.INT && der == Type.INT) return Type.INT;
        return null;
    }

    // true si dos tipos se pueden comparar con == o !=
    public static boolean sonComparables(Type izq, Type der) {
        if (izq == null || der == null) return true;
        if (izq == der) return true;
        return esNumerico(izq) && esNumerico(der);
    }

    // true si dos tipos se pueden comparar con < o >
    public static boolean sonOrdenables(Type izq, Type der) {
        if (izq == null || der == null) return true;
        return esNumerico(izq) && esNumerico(der);
    }
}
