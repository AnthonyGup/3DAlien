package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Unica fuente de verdad para los nombres internos (etiquetas) de funciones de
 * clase y constructores (regla Fase 0: Clase_metodo / Clase_init).
 */
public final class NombresInternos {

    private NombresInternos() {}

    /** Indice de la firma dentro del simbolo del metodo (0-based, orden de declaracion). */
    public static int indiceFirma(Symbol metodo, List<Type> firma) {
        if (metodo == null || firma == null) {
            return 0;
        }
        List<List<Type>> firmas = metodo.getFirmas();
        for (int i = 0; i < firmas.size(); i++) {
            if (mismaFirma(firmas.get(i), firma)) {
                return i;
            }
        }
        return 0;
    }

    /** Primera firma cuyo numero de parametros coincide (mismo criterio que el codegen). */
    public static int indicePorAridad(Symbol metodo, int argCount) {
        if (metodo == null) {
            return 0;
        }
        List<List<Type>> firmas = metodo.getFirmas();
        for (int i = 0; i < firmas.size(); i++) {
            if (firmas.get(i).size() == argCount) {
                return i;
            }
        }
        return 0;
    }

    public static String metodo(String clase, String nombre, int idx) {
        return clase + "_" + nombre + "_" + idx;
    }

    public static String constructor(String clase, int idx) {
        return clase + "_init_" + idx;
    }

    /** Etiqueta interna del metodo tal como la emite MethodDeclNode. */
    public static String etiquetaMetodo(String clase, String nombre, int idx) {
        return "Lf_" + sanitizar(metodo(clase, nombre, idx));
    }

    public static String etiquetaConstructor(String clase, int idx) {
        return "Lf_" + sanitizar(constructor(clase, idx));
    }

    public static int indiceConstructor(Symbol clase, Symbol ctor) {
        if (clase == null || ctor == null) {
            return 0;
        }
        List<Symbol> ctors = clase.getConstructores();
        for (int i = 0; i < ctors.size(); i++) {
            if (ctors.get(i) == ctor) {
                return i;
            }
        }
        return 0;
    }

    public static String sanitizar(String nombre) {
        StringBuilder sb = new StringBuilder();
        for (char ch : nombre.toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static boolean mismaFirma(List<Type> a, List<Type> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    public static List<Type> tiposDe(List<Symbol> params) {
        List<Type> tipos = new ArrayList<>();
        for (Symbol p : params) {
            tipos.add(p.getType());
        }
        return tipos;
    }
}
