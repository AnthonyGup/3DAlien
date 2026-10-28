package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

import java.util.List;
import java.util.Arrays;

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
            emitirIgualdadCadena();
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
                    ctx.emitirErrorSiNoCero("t6", "error_division");
                    ctx.emitir("    fdiv.d ft0, ft0, ft1");
                    guardar(op.getResult(), true, null, "ft0");
                }
                case "%" -> {
                    ctx.emitir("    fcvt.w.d t0, ft0, rtz");
                    ctx.emitir("    fcvt.w.d t1, ft1, rtz");
                    ctx.emitirErrorSiCero("t1", "error_division");
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
                ctx.emitirErrorSiCero("t1", "error_division");
                ctx.emitir("    div t2, t0, t1");
                guardar(op.getResult(), false, "t2", null);
            }
            case "%" -> {
                ctx.emitirErrorSiCero("t1", "error_division");
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

    private void emitirIgualdadCadena() {
        int n = ctx.siguienteSitioInline();
        String mismo = "L_str_eq_" + n + "_same";
        String falso = "L_str_eq_" + n + "_false";
        String fin = "L_str_eq_" + n + "_end";
        String bucle = "L_str_eq_" + n + "_loop";
        ctx.emitir("    beq a0, a1, " + mismo);
        ctx.emitir("    beqz a0, " + falso);
        ctx.emitir("    beqz a1, " + falso);
        ctx.emitir(bucle + ":");
        ctx.emitir("    lbu t0, 0(a0)");
        ctx.emitir("    lbu t1, 0(a1)");
        ctx.emitir("    bne t0, t1, " + falso);
        ctx.emitir("    beqz t0, " + mismo);
        ctx.emitir("    addi a0, a0, 1");
        ctx.emitir("    addi a1, a1, 1");
        ctx.emitir("    j " + bucle);
        ctx.emitir(mismo + ":");
        ctx.emitir("    li a0, 1");
        ctx.emitir("    j " + fin);
        ctx.emitir(falso + ":");
        ctx.emitir("    li a0, 0");
        ctx.emitir(fin + ":");
    }

    private void nativa(String simbolo, Operacion3D op) {
        emitirNativa(simbolo, Arrays.asList(op.getOperand1(), op.getOperand2()));
        guardar(op.getResult(), false, "a0", null);
    }

    public void emitirNativa(String simbolo, List<MemoryAccess> args) {
        switch (simbolo) {
            case "conc" -> {
                memoria.cargarEntero(args.get(0), "a0");
                memoria.cargarEntero(args.get(1), "a1");
                emitirConcatenacion();
            }
            case "strd" -> {
                memoria.cargarEntero(args.get(0), "a0");
                emitirEnteroAString();
            }
            case "strn" -> {
                memoria.cargarDouble(args.get(0), "fa0");
                ctx.emitir("    fcvt.w.d a0, fa0, rtz");
                emitirEnteroAString();
            }
            case "strc" -> {
                memoria.cargarEntero(args.get(0), "a0");
                emitirCaracterAString();
            }
            default -> ctx.error("nativa no soportada: " + simbolo);
        }
    }

    private void emitirConcatenacion() {
        int n = ctx.siguienteSitioInline();
        String primero = "L_conc_" + n + "_first";
        String segundo = "L_conc_" + n + "_second";
        String fin = "L_conc_" + n + "_end";
        String copiaA = "L_conc_" + n + "_copy_a";
        String copiaB = "L_conc_" + n + "_copy_b";
        ctx.emitir("    mv t2, s2");
        ctx.emitir("    beqz a0, " + segundo);
        ctx.emitir(copiaA + ":");
        ctx.emitir("    lbu t3, 0(a0)");
        ctx.emitir("    sb t3, 0(s2)");
        ctx.emitir("    addi a0, a0, 1");
        ctx.emitir("    addi s2, s2, 1");
        ctx.emitir("    bnez t3, " + copiaA);
        ctx.emitir("    addi s2, s2, -1");
        ctx.emitir(segundo + ":");
        ctx.emitir("    beqz a1, " + fin);
        ctx.emitir(copiaB + ":");
        ctx.emitir("    lbu t3, 0(a1)");
        ctx.emitir("    sb t3, 0(s2)");
        ctx.emitir("    addi a1, a1, 1");
        ctx.emitir("    addi s2, s2, 1");
        ctx.emitir("    bnez t3, " + copiaB);
        ctx.emitir(fin + ":");
        ctx.emitir("    mv a0, t2");
    }

    private void emitirCaracterAString() {
        ctx.emitir("    mv t0, s2");
        ctx.emitir("    sb a0, 0(s2)");
        ctx.emitir("    sb zero, 1(s2)");
        ctx.emitir("    addi s2, s2, 2");
        ctx.emitir("    mv a0, t0");
    }

    private void emitirEnteroAString() {
        int n = ctx.siguienteSitioInline();
        String positivo = "L_itoa_" + n + "_positive";
        String cero = "L_itoa_" + n + "_zero";
        String guardar = "L_itoa_" + n + "_save";
        String copiar = "L_itoa_" + n + "_copy";
        String fin = "L_itoa_" + n + "_end";
        ctx.emitir("    mv t6, s2");
        ctx.emitir("    addi sp, sp, -32");
        ctx.emitir("    bnez a0, " + positivo);
        ctx.emitir(cero + ":");
        ctx.emitir("    li t0, 48");
        ctx.emitir("    sb t0, 0(s2)");
        ctx.emitir("    sb zero, 1(s2)");
        ctx.emitir("    addi s2, s2, 2");
        ctx.emitir("    j " + fin);
        ctx.emitir(positivo + ":");
        ctx.emitir("    li t0, 0");
        ctx.emitir("    bgez a0, " + guardar);
        ctx.emitir("    li t1, 45");
        ctx.emitir("    sb t1, 0(s2)");
        ctx.emitir("    addi s2, s2, 1");
        ctx.emitir("    neg a0, a0");
        ctx.emitir(guardar + ":");
        ctx.emitir("    li t1, 10");
        ctx.emitir("    remu t2, a0, t1");
        ctx.emitir("    addi t2, t2, 48");
        ctx.emitir("    sb t2, 0(sp)");
        ctx.emitir("    addi sp, sp, 1");
        ctx.emitir("    addi t0, t0, 1");
        ctx.emitir("    divu a0, a0, t1");
        ctx.emitir("    bnez a0, " + guardar);
        ctx.emitir(copiar + ":");
        ctx.emitir("    addi sp, sp, -1");
        ctx.emitir("    lbu t2, 0(sp)");
        ctx.emitir("    sb t2, 0(s2)");
        ctx.emitir("    addi s2, s2, 1");
        ctx.emitir("    addi t0, t0, -1");
        ctx.emitir("    bnez t0, " + copiar);
        ctx.emitir("    sb zero, 0(s2)");
        ctx.emitir("    addi s2, s2, 1");
        ctx.emitir("    addi sp, sp, 32");
        ctx.emitir(fin + ":");
        ctx.emitir("    mv a0, t6");
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
