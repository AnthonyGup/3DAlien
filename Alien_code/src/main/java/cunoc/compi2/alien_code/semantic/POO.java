package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.Visibilidad;

public final class POO {

    private POO() {}

    public static Symbol buscarClase(ContextoSemantico ctx, String nombre) {
        if (nombre == null) return null;
        Symbol s = ctx.resolver(nombre);
        if (s != null && (s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA)) {
            return s;
        }
        return null;
    }

    public static boolean esSubtipo(ContextoSemantico ctx, String hijo, String padre) {
        if (hijo == null || padre == null) return false;
        if (hijo.equals(padre)) return true;
        Symbol actual = buscarClase(ctx, hijo);
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            Symbol sup = actual.getSuperclaseResuelta();
            if (sup == null) return false;
            if (sup.getName().equals(padre)) return true;
            actual = sup;
        }
        return false;
    }

    public static Symbol buscarAtributo(Symbol clase, String nombre) {
        Symbol actual = clase;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.getMiembros() != null) {
                Symbol m = actual.getMiembros().resolveLocal(nombre);
                if (m != null && m.getKind() == Symbol.Kind.VARIABLE) {
                    return m;
                }
            }
            actual = actual.getSuperclaseResuelta();
        }
        return null;
    }

    public static Symbol buscarAtributoIncluyendoPrivado(Symbol clase, String nombre) {
        Symbol actual = clase;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.getMiembros() != null) {
                Symbol m = actual.getMiembros().resolveLocal(nombre);
                if (m != null && m.getKind() == Symbol.Kind.VARIABLE) {
                    return m;
                }
            }
            actual = actual.getSuperclaseResuelta();
        }
        return null;
    }

    public static Symbol buscarMetodo(Symbol clase, String nombre) {
        Symbol actual = clase;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.getMiembros() != null) {
                Symbol m = actual.getMiembros().resolveLocal(nombre);
                if (m != null && m.getKind() == Symbol.Kind.METODO
                        && m.getVisibilidad() != Visibilidad.PRIVATE) {
                    return m;
                }
            }
            actual = actual.getSuperclaseResuelta();
        }
        return null;
    }

    public static Symbol buscarMetodoIncluyendoPrivado(Symbol clase, String nombre) {
        Symbol actual = clase;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.getMiembros() != null) {
                Symbol m = actual.getMiembros().resolveLocal(nombre);
                if (m != null && m.getKind() == Symbol.Kind.METODO) {
                    return m;
                }
            }
            actual = actual.getSuperclaseResuelta();
        }
        return null;
    }

    public static boolean puedeAcceder(Symbol miembro, Symbol contexto) {
        Visibilidad v = miembro.getVisibilidad();
        if (v == null || v == Visibilidad.PUBLIC || v == Visibilidad.DEFAULT) {
            return true;
        }
        if (contexto == null) {
            return false;
        }
        String duena = miembro.getClaseDuena();
        if (duena == null) {
            return true;
        }
        if (v == Visibilidad.PRIVATE) {
            return contexto.getName().equals(duena);
        }
        if (v == Visibilidad.PROTECTED) {
            if (contexto.getName().equals(duena)) return true;
            return esSubtipoDe(contexto, duena);
        }
        return true;
    }

    private static boolean esSubtipoDe(Symbol hijo, String padre) {
        Symbol actual = hijo;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.getName().equals(padre)) return true;
            actual = actual.getSuperclaseResuelta();
        }
        return false;
    }

    public static boolean esTipoClase(Type t) {
        return t == Type.CLASS || t == Type.STRUCT;
    }
}
