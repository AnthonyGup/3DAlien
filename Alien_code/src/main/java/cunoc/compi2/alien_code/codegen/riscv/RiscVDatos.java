package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.codegen.riscv.datos.PoolCadenas;
import cunoc.compi2.alien_code.codegen.riscv.datos.PoolConstantes;
import cunoc.compi2.alien_code.codegen.riscv.datos.PoolFormatos;

import java.util.Map;

public final class RiscVDatos {
    public static final int STACK_BYTES = 1 << 20;
    public static final int HEAP_BYTES = 1 << 21;
    public static final int STR_BYTES = 1 << 17;

    private final ContextoRiscV ctx;
    private final PoolCadenas cadenas = new PoolCadenas();
    private final PoolConstantes constantes = new PoolConstantes();
    private final PoolFormatos formatos = new PoolFormatos();

    public RiscVDatos(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void registrarCadena(String literal) {
        cadenas.registrar(literal);
    }

    public int baseDeCadena(String literal) {
        return cadenas.registrar(literal);
    }

    public int areaCadenas() {
        return cadenas.tamano();
    }

    public String etiquetaConstante(String texto) {
        int indice = constantes.registrar(texto);
        return constantes.etiqueta(indice);
    }

    public String etiquetaFormato(String texto) {
        int indice = formatos.registrar(texto);
        return formatos.etiqueta(indice);
    }

    public void emitir() {
        ctx.emitirDato(".data");
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Stack: .space " + STACK_BYTES);
        ctx.emitirDato("Heap:  .space " + HEAP_BYTES);
        ctx.emitirDato("Str:");
        for (Map.Entry<String, Integer> e : cadenas.entradas().entrySet()) {
            if (PoolCadenas.ascizSeguro(e.getKey())) {
                ctx.emitirDato("  " + PoolCadenas.asciz(PoolCadenas.contenido(e.getKey())));
            } else {
                ctx.emitirDato("  " + PoolCadenas.byteLine(e.getKey()));
            }
        }
        int resto = STR_BYTES - cadenas.tamano();
        if (resto > 0) {
            ctx.emitirDato("  .space " + resto);
        }
        ctx.emitirDato(".align 3");
        for (Map.Entry<String, Integer> e : constantes.entradas().entrySet()) {
            ctx.emitirDato(constantes.etiqueta(e.getValue()) + ": .double " + e.getKey());
        }
        for (Map.Entry<String, Integer> e : formatos.entradas().entrySet()) {
            if (PoolCadenas.ascizSeguro(e.getKey())) {
                ctx.emitirDato(formatos.etiqueta(e.getValue()) + ": " + PoolCadenas.asciz(e.getKey()));
            } else {
                ctx.emitirDato(formatos.etiqueta(e.getValue()) + ": " + PoolCadenas.byteLine(e.getKey()));
            }
        }
    }
}
