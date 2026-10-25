package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.ClaseLayout;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcula offsets en BYTES para globales, structs y clases.
 * No modifica ClaseLayouts (Fase 3); el backend RISC-V usa sus propios offsets.
 */
public final class OffsetsBytes {

    private OffsetsBytes() {}

    /** Alinea offset hacia arriba según alineación. */
    public static int alinear(int off, int alin) {
        return (off + alin - 1) & ~(alin - 1);
    }

    /** Offset en bytes de un global (sin clase/struct). */
    public static int offsetGlobal(Symbol s, int cursorGlobal) {
        TipoRV t = TipoRV.desde(s.getType());
        int off = alinear(cursorGlobal, t.alineacion);
        return off;
    }

    /** Tamano total de todos los globales en bytes. */
    public static int tamanoGlobales(java.util.List<Symbol> globales) {
        int cursor = 0;
        for (Symbol s : globales) {
            if (s.getKind() == Symbol.Kind.VARIABLE && !s.isField() && s.getOffset() >= 0) {
                cursor = offsetGlobal(s, cursor) + TipoRV.desde(s.getType()).tamano;
            }
        }
        return (cursor + 15) & ~15; // alinear a 16
    }

    /** Offsets en bytes de campos de un struct/clase (heredados primero). */
    public static Map<String, Integer> offsetsClase(ClaseLayouts layouts, String nombreClase) {
        Map<String, Integer> res = new LinkedHashMap<>();
        ClaseLayout l = layouts.layoutDe(nombreClase);
        if (l == null) return res;

        // Primero, heredados del padre
        if (l.padre != null) {
            res.putAll(offsetsClase(layouts, l.padre.nombre));
        }

        // Campos propios (usar el mapa de offsets del layout original en SLOTS,
        // pero convertir a bytes con alineación por tipo)
        // El layout original tiene offsets en slots (8 bytes c/u). Necesitamos
        // saber el tipo de cada campo. Como no lo tenemos aquí, haremos un
        // pass simple: iterar los campos declarados en orden y asignar.
        // Para la Etapa 2 basta con esto; la Etapa 6 usará la info real.

        // En su lugar, para la etapa 2 devolvemos mapa vacío; la Etapa 6
        // lo rellenará consultando la tabla de símbolos de la clase.
        return res;
    }

    /** Tamano total en bytes de una clase/struct (redondeado a múltiplo de 8). */
    public static int tamanoClase(ClaseLayouts layouts, String nombreClase) {
        ClaseLayout l = layouts.layoutDe(nombreClase);
        if (l == null) return 8;
        int slots = l.tamano;
        return (slots * 8 + 7) & ~7; // slots * 8 redondeado a 8
    }
}