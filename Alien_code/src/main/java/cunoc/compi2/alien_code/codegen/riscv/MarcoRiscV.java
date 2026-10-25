package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.ClaseLayouts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Allocator de marco de pila en BYTES (convención de clase).
 * Cada ranura = 4 bytes base; DOBLE ocupa 8 bytes y se alinea a 8.
 * Layout:
 *   0(fp)     = s0 anterior (guardado por el prólogo)
 *   -4(fp)    = ra (guardado por el prólogo)
 *   -8(fp)    = primer parametro / this
 *   -12(fp)   = segundo parametro ...
 *   ...       = locales y temporales
 *   (fondo)   = area de argumentos salientes (9.o en adelante)
 */
public final class MarcoRiscV {
    private int cursor = 4;            // bytes ya reservados (0(fp) y -4(fp) ocupados)
    private int salientes = 0;         // bytes para argumentos enteros 9 en adelante
    private final Map<MemoryAccess, Integer> offsetDe = new IdentityHashMap<>();
    private final Map<String, Integer> offsetParam = new LinkedHashMap<>();
    private int paramEnteros = 0;
    private int paramDoubles = 0;
    public String nombre;  // nombre de la función

    public MarcoRiscV() {}

    /** Reserva espacio para un valor y devuelve el offset respecto a fp (negativo). */
    public int reservar(TipoRV t) {
        cursor += (t == TipoRV.DOBLE) ? 8 : 4;
        if (t == TipoRV.DOBLE && cursor % 8 != 4) cursor += 4;   // alinear a 8
        return -cursor;
    }

    /** Registra un parámetro entrante con su tipo. */
    public int parametroEntrante(TipoRV t, String nombre) {
        int off = -8;
        if (t == TipoRV.DOBLE) {
            if (paramDoubles < 8) {
                off = -8; // se guardará en fa{paramDoubles}
                paramDoubles++;
            } else {
                // no soportado, error en etapa 5
                off = 0;
            }
        } else {
            if (paramEnteros < 8) {
                off = -8; // se guardará en a{paramEnteros}
                paramEnteros++;
            } else {
                // 9.o en adelante: en área de salientes
                off = salientes; // positivo desde sp
                salientes += 4;
            }
        }
        if (nombre != null) offsetParam.put(nombre, off);
        return off;
    }

    public void argumentosSalientes(int cantidadEnPila) {
        salientes = Math.max(salientes, 4 * cantidadEnPila);
    }

    /** Asigna offset a un MemoryAccess (local, temporal, parámetro). */
    public void asignarOffset(MemoryAccess op, TipoRV t) {
        int off = reservar(t);
        offsetDe.put(op, off);
    }

    public int getOffset(MemoryAccess op) {
        return offsetDe.getOrDefault(op, -1);
    }

    public int getOffsetParam(String nombre) {
        return offsetParam.getOrDefault(nombre, -999);
    }

    /** N = tamaño total del marco, siempre múltiplo de 16. */
    public int tamano() {
        return (cursor + 4 + salientes + 15) & ~15;
    }

    public int getParamEnteros() { return paramEnteros; }
    public int getParamDoubles() { return paramDoubles; }
    public int getSalientes() { return salientes; }
}