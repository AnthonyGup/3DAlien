package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.RiscVFormato;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

import java.util.List;

public final class EmisorES {
    private final ContextoRiscV ctx;
    private final EmisorMemoria memoria;

    public EmisorES(ContextoRiscV ctx, EmisorMemoria memoria) {
        this.ctx = ctx;
        this.memoria = memoria;
    }

    public void imprimir(Imprimir3D imp) {
        List<RiscVFormato.Trozo> trozos = new RiscVFormato().descomponer(imp);
        List<MemoryAccess> operandos = imp.getOperandos();
        List<String> especificadores = imp.getEspecificadores();
        int indice = 0;
        for (RiscVFormato.Trozo trozo : trozos) {
            if (trozo.tipo() == RiscVFormato.TipoTrozo.TEXTO) {
                if (!trozo.texto().isEmpty()) {
                    String et = ctx.registrarFormato(trozo.texto());
                    ctx.emitir("    la a0, " + et);
                    ctx.emitir("    jal ra, rt_print_cstr");
                }
                continue;
            }
            MemoryAccess op = operandos != null && indice < operandos.size() ? operandos.get(indice) : null;
            String esp = especificadores != null && indice < especificadores.size()
                    ? especificadores.get(indice) : trozo.texto();
            indice++;
            imprimirOperando(op, esp);
        }
    }

    private void imprimirOperando(MemoryAccess op, String esp) {
        String e = esp == null ? "%d" : esp.trim();
        TipoRV t = op != null ? ctx.getTipoRV(op) : TipoRV.ENTERO;
        switch (e) {
            case "%s" -> {
                memoria.cargarEntero(op, "a0");
                if (t == TipoRV.BOOL) {
                    ctx.emitir("    jal ra, rt_print_bool");
                } else {
                    ctx.emitir("    jal ra, rt_print_str");
                }
            }
            case "%c" -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    jal ra, rt_print_char");
            }
            case "%g", "%f", "%e" -> {
                memoria.cargarDouble(op, "fa0");
                ctx.emitir("    jal ra, rt_print_g");
            }
            case "%p" -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    jal ra, rt_print_ptr");
            }
            default -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    jal ra, rt_print_int");
            }
        }
    }

    public void leer(Leer3D leer) {
        String fmt = leer.getFormato() == null ? "%d" : leer.getFormato().replace(" ", "");
        MemoryAccess destino = leer.getResult();
        switch (fmt) {
            case "%s" -> {
                memoria.cargarEntero(destino, "a0");
                ctx.emitir("    jal ra, rt_read_str");
            }
            case "%c" -> {
                ctx.emitir("    jal ra, rt_read_char");
                memoria.guardarEntero(destino, "a0");
            }
            case "%lf", "%f", "%g" -> {
                ctx.emitir("    jal ra, rt_read_double");
                memoria.guardarDouble(destino, "fa0");
            }
            default -> {
                ctx.emitir("    jal ra, rt_read_int");
                memoria.guardarEntero(destino, "a0");
            }
        }
    }
}
