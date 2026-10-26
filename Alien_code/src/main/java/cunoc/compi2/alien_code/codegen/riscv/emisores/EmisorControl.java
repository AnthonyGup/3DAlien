package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Retornar3D;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.MarcoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

public final class EmisorControl {
    private final ContextoRiscV ctx;
    private final EmisorMemoria memoria;

    public EmisorControl(ContextoRiscV ctx, EmisorMemoria memoria) {
        this.ctx = ctx;
        this.memoria = memoria;
    }

    public void etiqueta(Etiqueta3D etiqueta) {
        ctx.emitir(nombre(etiqueta.getResult()) + ":");
    }

    public void salto(Goto3D salto) {
        ctx.emitir("    j " + nombre(salto.getResult()));
    }

    public void condicional(Condicional3D cond) {
        MemoryAccess op = cond.getOperand1();
        String destino = nombre(cond.getResult());
        if (op instanceof cunoc.compi2.alien_code.c3d.access.Literal3D lit) {
            String txt = lit.getTexto() == null ? "" : lit.getTexto().trim();
            int idx = indiceComparacion(txt);
            if (idx > 0) {
                String a = txt.substring(0, idx).trim();
                String b = txt.substring(idx + 1).trim();
                boolean omenor = b.startsWith("=");
                if (omenor) b = b.substring(1).trim();
                memoria.cargarTexto(a, "t0", 0);
                memoria.cargarTexto(b, "t1", 2);
                if (omenor) {
                    ctx.emitir("    slt t0, t1, t0");
                    ctx.emitir("    xori t0, t0, 1");
                } else {
                    ctx.emitir("    slt t0, t0, t1");
                }
                if ("if_true".equals(cond.operator())) {
                    ctx.emitir("    bnez t0, " + destino);
                } else {
                    ctx.emitir("    beqz t0, " + destino);
                }
                return;
            }
        }
        if (ctx.getTipoRV(op) == TipoRV.DOBLE) {
            memoria.cargarDouble(op, "ft0");
            ctx.emitir("    fcvt.d.w ft1, zero");
            ctx.emitir("    feq.d t0, ft0, ft1");
            ctx.emitir("    xori t0, t0, 1");
        } else {
            memoria.cargarEntero(op, "t0");
        }
        if ("if_true".equals(cond.operator())) {
            ctx.emitir("    bnez t0, " + destino);
        } else {
            ctx.emitir("    beqz t0, " + destino);
        }
    }

    private int indiceComparacion(String txt) {
        int prof = 0;
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if (c == '(' || c == '[') {
                prof++;
            } else if (c == ')' || c == ']') {
                prof--;
            } else if (c == '<' && prof == 0) {
                return i;
            }
        }
        return -1;
    }

    public void retornar(Retornar3D ret) {
        if (ret.getOperand1() != null) {
            if (ctx.getTipoRV(ret.getOperand1()) == TipoRV.DOBLE) {
                memoria.cargarDouble(ret.getOperand1(), "fa0");
            } else {
                memoria.cargarEntero(ret.getOperand1(), "a0");
            }
        }
        MarcoRiscV marco = ctx.getMarcoActual();
        String fin = marco == null || "main".equals(marco.nombre)
                ? "main_fin" : ctx.etiquetaFuncion(marco.nombre) + "_fin";
        ctx.emitir("    j " + fin);
    }

    public void detener(Halt3D halt) {
        ctx.emitir("    j main_fin");
    }

    private String nombre(MemoryAccess op) {
        if (op instanceof LabelAccess etiqueta) {
            return etiqueta.getNombre();
        }
        return "L0";
    }
}
