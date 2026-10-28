package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

public final class EmisorAritmetica {
    private final ContextoRiscV ctx;
    private final EmisorMemoria memoria;

    public EmisorAritmetica(ContextoRiscV ctx, EmisorMemoria memoria) {
        this.ctx = ctx;
        this.memoria = memoria;
    }

    public void operacion(Operacion3D op) {
        String simbolo = op.operator();
        if ("conc".equals(simbolo) || "strd".equals(simbolo)
                || "strn".equals(simbolo) || "strc".equals(simbolo)) {
            nativa(simbolo, op);
            return;
        }
        if (op.getOperand2() == null) {
            unaria(simbolo, op);
            return;
        }
        binaria(simbolo, op);
    }

    private void unaria(String simbolo, Operacion3D op) {
        MemoryAccess src = op.getOperand1();
        if (src == null) return;
        if ("-".equals(simbolo)) {
            if (ctx.getTipoRV(src) == TipoRV.DOBLE) {
                memoria.cargarDouble(src, "ft0");
                ctx.emitir("    fneg.d ft0, ft0");
                guardar(op.getResult(), true, null, "ft0");
            } else {
                memoria.cargarEntero(src, "t0");
                ctx.emitir("    neg t0, t0");
                guardar(op.getResult(), false, "t0", null);
            }
            return;
        }
        if ("!".equals(simbolo)) {
            if (ctx.getTipoRV(src) == TipoRV.DOBLE) {
                memoria.cargarDouble(src, "ft0");
                ctx.emitir("    fcvt.d.w ft1, zero");
                ctx.emitir("    feq.d t0, ft0, ft1");
            } else {
                memoria.cargarEntero(src, "t0");
                ctx.emitir("    seqz t0, t0");
            }
            guardar(op.getResult(), false, "t0", null);
            return;
        }
        ctx.error("operador unario no soportado: " + simbolo);
    }

    private void binaria(String simbolo, Operacion3D op) {
        MemoryAccess a = op.getOperand1();
        MemoryAccess b = op.getOperand2();
        boolean cadenas = a != null && b != null
                && (a.getTipo() == cunoc.compi2.alien_code.ast.Type.STRING
                || b.getTipo() == cunoc.compi2.alien_code.ast.Type.STRING
                || a.getTipo() == cunoc.compi2.alien_code.ast.Type.NULL
                || b.getTipo() == cunoc.compi2.alien_code.ast.Type.NULL);
        if (cadenas && ("==".equals(simbolo) || "!=".equals(simbolo))) {
            memoria.cargarEntero(a, "a0");
            memoria.cargarEntero(b, "a1");
            ctx.emitir("    jal ra, rt_str_eq");
            if ("!=".equals(simbolo)) {
                ctx.emitir("    xori a0, a0, 1");
            }
            guardar(op.getResult(), false, "a0", null);
            return;
        }
        boolean doble = ctx.getTipoRV(a) == TipoRV.DOBLE || ctx.getTipoRV(b) == TipoRV.DOBLE;
        if (doble) {
            memoria.cargarDouble(a, "ft0");
            memoria.cargarDouble(b, "ft1");
            switch (simbolo) {
                case "+" -> { ctx.emitir("    fadd.d ft0, ft0, ft1"); guardar(op.getResult(), true, null, "ft0"); }
                case "-" -> { ctx.emitir("    fsub.d ft0, ft0, ft1"); guardar(op.getResult(), true, null, "ft0"); }
                case "*" -> { ctx.emitir("    fmul.d ft0, ft0, ft1"); guardar(op.getResult(), true, null, "ft0"); }
                case "/" -> {
                    ctx.emitir("    fcvt.d.w ft2, zero");
                    ctx.emitir("    feq.d t6, ft1, ft2");
                    ctx.emitirErrorSiNoCero("t6", "rt_error_division");
                    ctx.emitir("    fdiv.d ft0, ft0, ft1");
                    guardar(op.getResult(), true, null, "ft0");
                }
                case "%" -> {
                    ctx.emitir("    fcvt.w.d t0, ft0, rtz");
                    ctx.emitir("    fcvt.w.d t1, ft1, rtz");
                    ctx.emitirErrorSiCero("t1", "rt_error_division");
                    ctx.emitir("    rem t0, t0, t1");
                    guardar(op.getResult(), false, "t0", null);
                }
                case "<" -> { ctx.emitir("    flt.d t0, ft0, ft1"); guardar(op.getResult(), false, "t0", null); }
                case ">" -> { ctx.emitir("    flt.d t0, ft1, ft0"); guardar(op.getResult(), false, "t0", null); }
                case "<=" -> { ctx.emitir("    fle.d t0, ft0, ft1"); guardar(op.getResult(), false, "t0", null); }
                case ">=" -> { ctx.emitir("    fle.d t0, ft1, ft0"); guardar(op.getResult(), false, "t0", null); }
                case "==" -> { ctx.emitir("    feq.d t0, ft0, ft1"); guardar(op.getResult(), false, "t0", null); }
                case "!=" -> {
                    ctx.emitir("    feq.d t0, ft0, ft1");
                    ctx.emitir("    xori t0, t0, 1");
                    guardar(op.getResult(), false, "t0", null);
                }
                case "&&", "||" -> {
                    ctx.emitir("    fcvt.d.w ft2, zero");
                    ctx.emitir("    feq.d t0, ft0, ft2");
                    ctx.emitir("    xori t0, t0, 1");
                    ctx.emitir("    feq.d t1, ft1, ft2");
                    ctx.emitir("    xori t1, t1, 1");
                    ctx.emitir("    " + ("&&".equals(simbolo) ? "and t0, t0, t1" : "or t0, t0, t1"));
                    guardar(op.getResult(), false, "t0", null);
                }
                default -> ctx.error("operador no soportado: " + simbolo);
            }
            return;
        }
        memoria.cargarEntero(a, "t0", 0);
        memoria.cargarEntero(b, "t1", 2);
        switch (simbolo) {
            case "+" -> { ctx.emitir("    add t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            case "-" -> { ctx.emitir("    sub t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            case "*" -> { ctx.emitir("    mul t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            case "/" -> {
                ctx.emitirErrorSiCero("t1", "rt_error_division");
                ctx.emitir("    div t2, t0, t1");
                guardar(op.getResult(), false, "t2", null);
            }
            case "%" -> {
                ctx.emitirErrorSiCero("t1", "rt_error_division");
                ctx.emitir("    rem t2, t0, t1");
                guardar(op.getResult(), false, "t2", null);
            }
            case "<" -> { ctx.emitir("    slt t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            case ">" -> { ctx.emitir("    slt t2, t1, t0"); guardar(op.getResult(), false, "t2", null); }
            case "<=" -> {
                ctx.emitir("    slt t2, t1, t0");
                ctx.emitir("    xori t2, t2, 1");
                guardar(op.getResult(), false, "t2", null);
            }
            case ">=" -> {
                ctx.emitir("    slt t2, t0, t1");
                ctx.emitir("    xori t2, t2, 1");
                guardar(op.getResult(), false, "t2", null);
            }
            case "==" -> {
                ctx.emitir("    sub t2, t0, t1");
                ctx.emitir("    seqz t2, t2");
                guardar(op.getResult(), false, "t2", null);
            }
            case "!=" -> {
                ctx.emitir("    sub t2, t0, t1");
                ctx.emitir("    snez t2, t2");
                guardar(op.getResult(), false, "t2", null);
            }
            case "&&" -> { ctx.emitir("    and t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            case "||" -> { ctx.emitir("    or t2, t0, t1"); guardar(op.getResult(), false, "t2", null); }
            default -> ctx.error("operador no soportado: " + simbolo);
        }
    }

    private void nativa(String simbolo, Operacion3D op) {
        switch (simbolo) {
            case "conc" -> {
                memoria.cargarEntero(op.getOperand1(), "a0");
                memoria.cargarEntero(op.getOperand2(), "a1");
                ctx.emitir("    jal ra, rt_conc");
            }
            case "strd" -> {
                memoria.cargarEntero(op.getOperand1(), "a0");
                ctx.emitir("    jal ra, rt_strd");
            }
            case "strn" -> {
                memoria.cargarDouble(op.getOperand1(), "fa0");
                ctx.emitir("    jal ra, rt_strn");
            }
            case "strc" -> {
                memoria.cargarEntero(op.getOperand1(), "a0");
                ctx.emitir("    jal ra, rt_strc");
            }
            default -> ctx.error("nativa no soportada: " + simbolo);
        }
        guardar(op.getResult(), false, "a0", null);
    }

    private void guardar(MemoryAccess res, boolean enDouble, String regI, String regF) {
        if (res == null) return;
        TipoRV tr = ctx.getTipoRV(res);
        if (enDouble) {
            if (tr == TipoRV.DOBLE) {
                memoria.guardarDouble(res, regF);
            } else {
                ctx.emitir("    fcvt.w.d t0, " + regF + ", rtz");
                memoria.guardarEntero(res, "t0");
            }
        } else {
            if (tr == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w ft0, " + regI);
                memoria.guardarDouble(res, "ft0");
            } else {
                memoria.guardarEntero(res, regI);
            }
        }
    }
}
