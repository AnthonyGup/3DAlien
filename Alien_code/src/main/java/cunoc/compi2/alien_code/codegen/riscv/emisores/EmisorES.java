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
    private final RiscVFormato formato = new RiscVFormato();

    public EmisorES(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void imprimir(Imprimir3D imp) {
        List<RiscVFormato.Trozo> trozos = formato.descomponer(imp);
        List<MemoryAccess> operandos = imp.getOperandos();
        List<String> especificadores = imp.getEspecificadores();
        int indice = 0;
        for (RiscVFormato.Trozo trozo : trozos) {
            if (trozo.tipo() == RiscVFormato.TipoTrozo.TEXTO) {
                if (!trozo.texto().isEmpty()) {
                    String et = ctx.etiquetaFormato(trozo.texto());
                    if (et == null) {
                        et = "fmt_" + ctx.nuevaEtiquetaFormato();
                        ctx.setEtiquetaFormato(trozo.texto(), et);
                    }
                    ctx.emitir("    la a0, " + et);
                    ctx.emitir("    jal ra, rt_print_cstr");
                }
                continue;
            }
            MemoryAccess op = operandos != null && indice < operandos.size() ? operandos.get(indice) : null;
            String esp = especificadores != null && indice < especificadores.size()
                    ? especificadores.get(indice) : "%d";
            indice++;
            imprimirOperando(op, esp);
        }
        if (operandos != null && indice < operandos.size()) {
            for (int i = indice; i < operandos.size(); i++) {
                imprimirOperando(operandos.get(i),
                        especificadores != null && i < especificadores.size()
                                ? especificadores.get(i) : "%d");
            }
        }
    }

    private void imprimirOperando(MemoryAccess op, String esp) {
        String especificador = esp == null ? "%d" : esp.trim();
        if (op == null) {
            ctx.emitir("    li a0, 0");
            ctx.emitir("    jal ra, rt_print_int");
            return;
        }
        TipoRV t = ctx.getTipoRV(op);
        switch (especificador) {
            case "%s" -> {
                if (t == TipoRV.BOOL) {
                    cargar(op, "a0", TipoRV.BOOL);
                    ctx.emitir("    jal ra, rt_print_bool");
                } else {
                    cargar(op, "a0", TipoRV.REF);
                    ctx.emitir("    jal ra, rt_print_str");
                }
            }
            case "%c" -> {
                cargar(op, "a0", TipoRV.CHAR);
                ctx.emitir("    jal ra, rt_print_char");
            }
            case "%g" -> {
                cargar(op, "fa0", TipoRV.DOBLE);
                ctx.emitir("    jal ra, rt_print_g");
            }
            case "%p" -> {
                cargar(op, "a0", TipoRV.REF);
                ctx.emitir("    jal ra, rt_print_ptr");
            }
            default -> {
                // %d y otros: entero
                cargar(op, "a0", TipoRV.ENTERO);
                ctx.emitir("    jal ra, rt_print_int");
            }
        }
    }

    public void leer(Leer3D leer) {
        String fmt = leer.getFormato() == null ? "" : leer.getFormato().trim();
        MemoryAccess res = leer.getResult();
        TipoRV tRes = ctx.getTipoRV(res);
        switch (fmt) {
            case "%s" -> {
                // malloc 256 bytes en Str (s2)
                ctx.emitir("    mv t0, s2");
                ctx.emitir("    addi s2, s2, 256");
                ctx.emitir("    mv a0, t0");
                ctx.emitir("    jal ra, rt_read_str");
                // guardar dirección en resultado
                guardar(res, TipoRV.REF, "t0");
            }
            case "%lf" -> {
                ctx.emitir("    jal ra, rt_read_double");
                guardar(res, TipoRV.DOBLE, "fa0");
            }
            case "%c" -> {
                ctx.emitir("    jal ra, rt_read_char");
                guardar(res, TipoRV.CHAR, "a0");
            }
            default -> { // %d
                ctx.emitir("    jal ra, rt_read_int");
                guardar(res, TipoRV.ENTERO, "a0");
            }
        }
    }

    private void cargar(MemoryAccess op, String reg, TipoRV t) {
        if (op == null) {
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + reg + ", zero");
            else ctx.emitir("    li " + reg + ", 0");
            return;
        }
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    fld " + reg + ", " + ctx.getOffset(op) + "(s0)");
        } else {
            ctx.emitir("    lw " + reg + ", " + ctx.getOffset(op) + "(s0)");
        }
    }

    private void guardar(MemoryAccess dest, TipoRV t, String srcReg) {
        if (dest instanceof cunoc.compi2.alien_code.c3d.access.NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                int off = -8;
                if (t == TipoRV.DOBLE) ctx.emitir("    fsd " + srcReg + ", " + off + "(s0)");
                else ctx.emitir("    sw " + srcReg + ", " + off + "(s0)");
                return;
            }
            if (na.isGlobal()) {
                int off = ctx.getOffsetGlobal(na.getNombre());
                if (t == TipoRV.DOBLE) ctx.emitir("    fsd " + srcReg + ", " + off + "(s0)");
                else ctx.emitir("    sw " + srcReg + ", " + off + "(s0)");
                return;
            }
        }
        int off = ctx.getOffset(dest);
        if (t == TipoRV.DOBLE) ctx.emitir("    fsd " + srcReg + ", " + off + "(s0)");
        else ctx.emitir("    sw " + srcReg + ", " + off + "(s0)");
    }
}