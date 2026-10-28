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
                    ctx.emitir("    li a7, 4");
                    ctx.emitir("    ecall");
                }
                continue;
            }
            MemoryAccess op = operandos != null && indice < operandos.size() ? operandos.get(indice) : null;
            String esp = especificadores != null && indice < especificadores.size()
                    ? especificadores.get(indice) : trozo.texto();
            indice++;
            imprimirOperando(op, esp);
        }
        ctx.emitir("    li a0, 10");
        ctx.emitir("    li a7, 11");
        ctx.emitir("    ecall");
    }

    private void imprimirOperando(MemoryAccess op, String esp) {
        String e = esp == null ? "%d" : esp.trim();
        TipoRV t = op != null ? ctx.getTipoRV(op) : TipoRV.ENTERO;
        switch (e) {
            case "%s" -> {
                memoria.cargarEntero(op, "a0");
                if (t == TipoRV.BOOL) {
                    emitirBooleano();
                } else {
                    ctx.emitir("    li a7, 4");
                    ctx.emitir("    ecall");
                }
            }
            case "%c" -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    li a7, 11");
                ctx.emitir("    ecall");
            }
            case "%g", "%f", "%e" -> {
                memoria.cargarDouble(op, "fa0");
                ctx.emitir("    li a7, 3");
                ctx.emitir("    ecall");
            }
            case "%p" -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    li a7, 34");
                ctx.emitir("    ecall");
            }
            default -> {
                memoria.cargarEntero(op, "a0");
                ctx.emitir("    li a7, 1");
                ctx.emitir("    ecall");
            }
        }
    }

    private void emitirBooleano() {
            int n = ctx.siguienteSitioInline();
            String fin = "L_bool_" + n + "_end";
            String si = ctx.registrarCadena("true");
            String no = ctx.registrarCadena("false");
            ctx.emitir("    bnez a0, " + fin + "_true");
            ctx.emitir("    la a0, " + no);
            ctx.emitir("    j " + fin);
            ctx.emitir(fin + "_true:");
            ctx.emitir("    la a0, " + si);
            ctx.emitir(fin + ":");
            ctx.emitir("    li a7, 4");
            ctx.emitir("    ecall");
    }

    public void leer(Leer3D leer) {
        String fmt = leer.getFormato() == null ? "%d" : leer.getFormato().replace(" ", "");
        MemoryAccess destino = leer.getResult();
        switch (fmt) {
            case "%s" -> {
                ctx.emitir("    mv a0, s2");
                ctx.emitir("    li a1, 1024");
                ctx.emitir("    li a7, 8");
                ctx.emitir("    ecall");
                ctx.emitir("    mv t0, s2");
                String fin = "L_read_str_" + ctx.siguienteSitioInline();
                ctx.emitir(fin + "_loop:");
                ctx.emitir("    lbu t1, 0(s2)");
                ctx.emitir("    li t2, 10");
                ctx.emitir("    beq t1, t2, " + fin + "_trim");
                ctx.emitir("    li t2, 13");
                ctx.emitir("    beq t1, t2, " + fin + "_trim");
                ctx.emitir("    addi s2, s2, 1");
                ctx.emitir("    bnez t1, " + fin + "_loop");
                ctx.emitir("    j " + fin + "_done");
                ctx.emitir(fin + "_trim:");
                ctx.emitir("    sb zero, 0(s2)");
                ctx.emitir("    addi s2, s2, 1");
                ctx.emitir(fin + "_done:");
                memoria.guardarEntero(destino, "t0");
            }
            case "%c" -> {
                ctx.emitir("    li a7, 12");
                ctx.emitir("    ecall");
                memoria.guardarEntero(destino, "a0");
            }
            case "%lf", "%f", "%g" -> {
                ctx.emitir("    li a7, 7");
                ctx.emitir("    ecall");
                memoria.guardarDouble(destino, "fa0");
            }
            default -> {
                ctx.emitir("    li a7, 5");
                ctx.emitir("    ecall");
                memoria.guardarEntero(destino, "a0");
            }
        }
    }
}
