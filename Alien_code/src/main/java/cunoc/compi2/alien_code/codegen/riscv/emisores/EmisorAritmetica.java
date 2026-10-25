package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.codegen.riscv.AccesoMemoria;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.OperadorRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

public final class EmisorAritmetica {
    private final ContextoRiscV ctx;

    public EmisorAritmetica(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void operacion(Operacion3D op) {
        String simbolo = op.operator();
        if ("conc".equals(simbolo) || "strd".equals(simbolo) || "strn".equals(simbolo) || "strc".equals(simbolo)) {
            concatenacion(simbolo, op);
            return;
        }
        OperadorRiscV operador = OperadorRiscV.desde(simbolo);
        if (operador == null) {
            ctx.error("operador no soportado: " + simbolo);
            return;
        }
        if (op.getOperand1() == null) {
            ctx.error("operacion sin operando izquierdo: " + simbolo);
            return;
        }
        if (op.getOperand2() == null) {
            unaria(operador, op);
            return;
        }
        binaria(operador, op);
    }

    private void unaria(OperadorRiscV operador, Operacion3D op) {
        MemoryAccess src = op.getOperand1();
        TipoRV t = ctx.getTipoRV(src);
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    fld ft0, " + offsetReg(src) + "(s0)");
        } else {
            ctx.emitir("    lw t0, " + offsetReg(src) + "(s0)");
        }
        switch (operador) {
            case NEGACION -> {
                if (t == TipoRV.DOBLE) ctx.emitir("    fneg.d ft0, ft0");
                else ctx.emitir("    neg t0, t0");
            }
            case NO -> {
                if (t.esEntero()) {
                    ctx.emitir("    seqz t0, t0");
                } else {
                    ctx.emitir("    fcvt.d.w ft0, zero");
                    ctx.emitir("    feq.d t1, ft0, ft0");
                    ctx.emitir("    seqz t0, t1");
                }
            }
            default -> ctx.error("operador unario no soportado: " + operador.simbolo());
        }
        if (op.getResult() != null) {
            TipoRV tDest = ctx.getTipoRV(op.getResult());
            String srcReg = (t == TipoRV.DOBLE) ? "ft0" : "t0";
            if (t != tDest) convertir(srcReg, t, tDest);
            guardarResultado(op.getResult(), tDest, srcReg);
        }
    }

    private void binaria(OperadorRiscV operador, Operacion3D op) {
        MemoryAccess op1 = op.getOperand1();
        MemoryAccess op2 = op.getOperand2();
        TipoRV t1 = ctx.getTipoRV(op1);
        TipoRV t2 = ctx.getTipoRV(op2);
        TipoRV tRes = ctx.getTipoRV(op.getResult());

        // Promover a double si el resultado es double
        boolean usarDouble = tRes == TipoRV.DOBLE || t1 == TipoRV.DOBLE || t2 == TipoRV.DOBLE;

        if (usarDouble) {
            // Cargar ambos como double
            cargarComo(op1, "fs0", t1, TipoRV.DOBLE);
            cargarComo(op2, "ft0", t2, TipoRV.DOBLE);
            switch (operador) {
                case SUMA -> ctx.emitir("    fadd.d ft0, fs0, ft0");
                case RESTA -> ctx.emitir("    fsub.d ft0, fs0, ft0");
                case MULTIPLICACION -> ctx.emitir("    fmul.d ft0, fs0, ft0");
                case DIVISION -> ctx.emitir("    fdiv.d ft0, fs0, ft0");
                case RESIDUO -> {
                    ctx.emitir("    fcvt.w.d t0, fs0, rtz");
                    ctx.emitir("    fcvt.w.d t1, ft0, rtz");
                    ctx.emitir("    rem t0, t0, t1");
                    ctx.emitir("    fcvt.d.w ft0, t0");
                }
                case MENOR -> ctx.emitir("    flt.d t0, fs0, ft0");
                case MAYOR -> ctx.emitir("    flt.d t0, ft0, fs0");
                case MENOR_IGUAL -> ctx.emitir("    fle.d t0, fs0, ft0");
                case MAYOR_IGUAL -> ctx.emitir("    fle.d t0, ft0, fs0");
                case IGUAL -> ctx.emitir("    feq.d t0, fs0, ft0");
                case DISTINTO -> {
                    ctx.emitir("    feq.d t0, fs0, ft0");
                    ctx.emitir("    xori t0, t0, 1");
                }
                case AND, OR -> {
                    ctx.emitir("    fcvt.w.d t0, fs0, rtz");
                    ctx.emitir("    fcvt.w.d t1, ft0, rtz");
                    ctx.emitir("    " + (operador == OperadorRiscV.AND ? "and" : "or") + " t0, t0, t1");
                    ctx.emitir("    snez t0, t0");
                    ctx.emitir("    fcvt.d.w ft0, t0");
                }
            }
            // Comparaciones dejan resultado en t0 (entero 0/1), convertir si resultado es double
            if (operador == OperadorRiscV.MENOR || operador == OperadorRiscV.MAYOR
                    || operador == OperadorRiscV.MENOR_IGUAL || operador == OperadorRiscV.MAYOR_IGUAL
                    || operador == OperadorRiscV.IGUAL || operador == OperadorRiscV.DISTINTO) {
                if (tRes == TipoRV.DOBLE) {
                    ctx.emitir("    fcvt.d.w ft0, t0");
                }
            }
        } else {
            // Enteros
            ctx.emitir("    lw t0, " + offsetReg(op1) + "(s0)");
            ctx.emitir("    lw t1, " + offsetReg(op2) + "(s0)");
            switch (operador) {
                case SUMA -> ctx.emitir("    add t2, t0, t1");
                case RESTA -> ctx.emitir("    sub t2, t0, t1");
                case MULTIPLICACION -> ctx.emitir("    mul t2, t0, t1");
                case DIVISION -> ctx.emitir("    div t2, t0, t1");
                case RESIDUO -> ctx.emitir("    rem t2, t0, t1");
                case MENOR -> ctx.emitir("    slt t2, t0, t1");
                case MAYOR -> ctx.emitir("    slt t2, t1, t0");
                case MENOR_IGUAL -> { ctx.emitir("    slt t2, t1, t0"); ctx.emitir("    xori t2, t2, 1"); }
                case MAYOR_IGUAL -> { ctx.emitir("    slt t2, t0, t1"); ctx.emitir("    xori t2, t2, 1"); }
                case IGUAL -> { ctx.emitir("    sub t2, t0, t1"); ctx.emitir("    seqz t2, t2"); }
                case DISTINTO -> { ctx.emitir("    sub t2, t0, t1"); ctx.emitir("    snez t2, t2"); }
                case AND -> ctx.emitir("    and t2, t0, t1");
                case OR -> ctx.emitir("    or t2, t0, t1");
                default -> { ctx.error("operador binario no soportado: " + operador.simbolo()); return; }
            }
            // Guardar resultado
            if (tRes == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w ft0, t2");
            } else {
                ctx.emitir("    mv t0, t2");
            }
        }
        if (op.getResult() != null) {
            String srcReg = (tRes == TipoRV.DOBLE) ? "ft0" : "t0";
            guardarResultado(op.getResult(), tRes, srcReg);
        }
    }

    private void concatenacion(String simbolo, Operacion3D op) {
        // conc: a0=dir1, a1=dir2 -> rt_conc -> a0=dirResultado
        // strd: a0=int -> rt_strd -> a0=dir
        // strn: fa0=double -> rt_strn -> a0=dir
        // strc: a0=char -> rt_strc -> a0=dir
        switch (simbolo) {
            case "conc" -> {
                cargarComo(op.getOperand1(), "a0", ctx.getTipoRV(op.getOperand1()), TipoRV.REF);
                cargarComo(op.getOperand2(), "a1", ctx.getTipoRV(op.getOperand2()), TipoRV.REF);
                ctx.emitir("    jal ra, rt_conc");
            }
            case "strd" -> {
                cargarComo(op.getOperand1(), "a0", ctx.getTipoRV(op.getOperand1()), TipoRV.ENTERO);
                ctx.emitir("    jal ra, rt_strd");
            }
            case "strn" -> {
                cargarComo(op.getOperand1(), "fa0", ctx.getTipoRV(op.getOperand1()), TipoRV.DOBLE);
                ctx.emitir("    jal ra, rt_strn");
            }
            case "strc" -> {
                cargarComo(op.getOperand1(), "a0", ctx.getTipoRV(op.getOperand1()), TipoRV.CHAR);
                ctx.emitir("    jal ra, rt_strc");
            }
        }
        if (op.getResult() != null) {
            TipoRV tRes = ctx.getTipoRV(op.getResult());
            String srcReg = (tRes == TipoRV.DOBLE) ? "ft0" : "a0";
            if (tRes != TipoRV.REF) {
                // rt_* devuelven dirección en a0, convertir si necesario
                if (tRes == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w ft0, a0");
                else ctx.emitir("    mv t0, a0");
            }
            guardarResultado(op.getResult(), tRes, srcReg);
        }
    }

    private void cargarComo(MemoryAccess op, String reg, TipoRV tSrc, TipoRV tDst) {
        if (tSrc == tDst) {
            ctx.emitir("    " + tDst.carga + " " + reg + ", " + offsetReg(op) + "(s0)");
        } else {
            // Cargar en registro temporal y convertir
            String tmp = (tSrc == TipoRV.DOBLE) ? "ft0" : "t0";
            ctx.emitir("    " + tSrc.carga + " " + tmp + ", " + offsetReg(op) + "(s0)");
            if (tSrc.esEntero() && tDst == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w " + reg + ", " + tmp);
            } else if (tSrc == TipoRV.DOBLE && tDst.esEntero()) {
                ctx.emitir("    fcvt.w.d " + reg + ", " + tmp + ", rtz");
            } else {
                ctx.emitir("    mv " + reg + ", " + tmp);
            }
        }
    }

    private void guardarResultado(MemoryAccess dest, TipoRV t, String srcReg) {
        if (dest instanceof cunoc.compi2.alien_code.c3d.access.NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", -8);
                return;
            }
            if (na.isGlobal()) {
                int off = ctx.getOffsetGlobal(na.getNombre());
                AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
                return;
            }
        }
        int off = ctx.getOffset(dest);
        AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
    }

    private int offsetReg(MemoryAccess op) {
        return ctx.getOffset(op);
    }

    private void convertir(String reg, TipoRV de, TipoRV a) {
        if (de == a) return;
        if (de.esEntero() && a == TipoRV.DOBLE) {
            ctx.emitir("    fcvt.d.w " + reg + ", " + reg);
        } else if (de == TipoRV.DOBLE && a.esEntero()) {
            ctx.emitir("    fcvt.w.d " + reg + ", " + reg + ", rtz");
        }
    }
}