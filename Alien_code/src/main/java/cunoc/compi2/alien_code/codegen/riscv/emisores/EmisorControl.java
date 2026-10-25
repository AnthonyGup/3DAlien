package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Retornar3D;
import cunoc.compi2.alien_code.codegen.riscv.AccesoMemoria;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.MarcoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

public final class EmisorControl {
    private final ContextoRiscV ctx;

    public EmisorControl(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void etiqueta(Etiqueta3D etiqueta) {
        ctx.emitir(nombre(etiqueta.getResult()) + ":");
    }

    public void salto(Goto3D salto) {
        ctx.emitir("    j " + nombre(salto.getResult()));
    }

    public void condicional(Condicional3D cond) {
        MemoryAccess op = cond.getOperand1();
        TipoRV t = ctx.getTipoRV(op);
        // Cargar como entero 0/1
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    fld ft0, " + offset(op) + "(s0)");
            ctx.emitir("    fcvt.d.w ft1, zero");
            ctx.emitir("    feq.d t0, ft0, ft1");
        } else {
            ctx.emitir("    lw t0, " + offset(op) + "(s0)");
            ctx.emitir("    beqz t0, L_zero_check");
            ctx.emitir("    li t0, 1");
            ctx.emitir("    j L_after_check");
            ctx.emitir("L_zero_check:");
            ctx.emitir("    li t0, 0");
            ctx.emitir("L_after_check:");
        }
        if ("if_true".equals(cond.operator())) {
            ctx.emitir("    xori t0, t0, 1");
        }
        ctx.emitir("    bnez t0, " + nombre(cond.getResult()));
    }

    public void retornar(Retornar3D ret) {
        if (ret.getOperand1() != null) {
            MemoryAccess op = ret.getOperand1();
            TipoRV t = ctx.getTipoRV(op);
            if (t == TipoRV.DOBLE) {
                ctx.emitir("    fld fa0, " + offset(op) + "(s0)");
            } else {
                ctx.emitir("    lw a0, " + offset(op) + "(s0)");
            }
        }
        // Saltar al epílogo de la función actual
        MarcoRiscV marco = ctx.getMarcoActual();
        if (marco != null && "main".equals(marco.nombre)) {
            ctx.emitir("    jal ra, rt_exit");
        } else if (marco != null) {
            ctx.emitir("    j " + marco.nombre + "_fin");
        } else {
            ctx.emitir("    jal ra, rt_exit");
        }
    }

    public void detener(Halt3D halt) {
        ctx.emitir("    jal ra, rt_exit");
    }

    public void inicioFuncion(InicioFuncion3D inicio) {
        String nombre = inicio.getNombre();
        if ("main".equals(nombre)) {
            // main también necesita prólogo con frame
            MarcoRiscV marco = ctx.marcos().get("main");
            if (marco == null) {
                marco = new MarcoRiscV();
                marco.nombre = "main";
                ctx.marcos().put("main", marco);
            }
            ctx.setMarcoActual(marco);
            int N = marco.tamano();
            emitirPrologo(marco, N);
            return;
        }
        MarcoRiscV marco = ctx.marcos().get(nombre);
        if (marco == null) {
            marco = new MarcoRiscV();
            ctx.marcos().put(nombre, marco);
        }
        ctx.setMarcoActual(marco);
        marco.nombre = nombre;

        // Analizar parámetros desde la descripción
        String desc = inicio.getDescripcion();
        int slot = 3; // P+3 = primer parámetro
        if (desc != null && !desc.isBlank()) {
            for (String p : desc.split(";")) {
                String nom = p.trim();
                String tipoStr = "";
                int idx = nom.indexOf(':');
                if (idx > 0) {
                    tipoStr = nom.substring(idx + 1).trim();
                    nom = nom.substring(0, idx).trim();
                }
                if (nom.isEmpty()) continue;
                TipoRV t = TipoRV.desdeString(tipoStr);
                if ("self".equals(nom)) {
                    marco.parametroEntrante(TipoRV.REF, "self");
                } else {
                    marco.parametroEntrante(t, nom);
                }
            }
        }
        int N = marco.tamano();
        emitirPrologo(marco, N);

        // Guardar parámetros entrantes en sus ranuras
        if (desc != null && !desc.isBlank()) {
            for (String p : desc.split(";")) {
                String nom = p.trim();
                String tipoStr = "";
                int idx = nom.indexOf(':');
                if (idx > 0) {
                    tipoStr = nom.substring(idx + 1).trim();
                    nom = nom.substring(0, idx).trim();
                }
                if (nom.isEmpty()) continue;
                TipoRV t = TipoRV.desdeString(tipoStr);
                int paramReg = marco.getOffsetParam(nom);
                if (t == TipoRV.DOBLE) {
                    ctx.emitir("    fsd fa" + (marco.getParamDoubles() - 1) + ", " + paramReg + "(s0)");
                } else {
                    ctx.emitir("    sw a" + (marco.getParamEnteros() - 1) + ", " + paramReg + "(s0)");
                }
            }
        }
    }

    private void emitirPrologo(MarcoRiscV marco, int N) {
        ctx.emitir(marco.nombre + ":");
        if (N <= 2047) {
            ctx.emitir("    addi sp, sp, -" + N);
            ctx.emitir("    sw s0, " + (N - 4) + "(sp)");
            ctx.emitir("    sw ra, " + (N - 8) + "(sp)");
            ctx.emitir("    addi s0, sp, " + (N - 4));
        } else {
            ctx.emitir("    li t6, -" + N);
            ctx.emitir("    add sp, sp, t6");
            ctx.emitir("    li t6, " + (N - 4));
            ctx.emitir("    add t6, sp, t6");
            ctx.emitir("    sw s0, 0(t6)");
            ctx.emitir("    li t6, " + (N - 8));
            ctx.emitir("    add t6, sp, t6");
            ctx.emitir("    sw ra, 0(t6)");
            ctx.emitir("    li t6, " + (N - 4));
            ctx.emitir("    add s0, sp, t6");
        }
    }

    public void finFuncion(FinFuncion3D fin) {
        MarcoRiscV marco = ctx.getMarcoActual();
        if (marco == null) return;
        String finLabel = marco.nombre + "_fin";
        ctx.emitir(finLabel + ":");
        int N = marco.tamano();
        if (N <= 2047) {
            ctx.emitir("    lw ra, -4(s0)");
            ctx.emitir("    lw s0, 0(s0)");
            ctx.emitir("    addi sp, sp, " + N);
        } else {
            ctx.emitir("    lw ra, -4(s0)");
            ctx.emitir("    lw s0, 0(s0)");
            ctx.emitir("    li t6, " + N);
            ctx.emitir("    add sp, sp, t6");
        }
        if ("main".equals(marco.nombre)) {
            ctx.emitir("    jal ra, rt_exit");
        } else {
            ctx.emitir("    ret");
        }
        ctx.setMarcoActual(null);
    }

    private int offset(MemoryAccess op) {
        return ctx.getOffset(op);
    }

    private String nombre(MemoryAccess op) {
        if (op instanceof LabelAccess etiqueta) {
            return etiqueta.getNombre();
        }
        return "L0";
    }
}