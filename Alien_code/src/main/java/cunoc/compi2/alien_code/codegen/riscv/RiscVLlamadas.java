package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;

import java.util.List;
import java.util.Map;

public final class RiscVLlamadas {
    private final ContextoRiscV ctx;
    private final RiscVMemoria memoria;

    public RiscVLlamadas(ContextoRiscV ctx, RiscVMemoria memoria) {
        this.ctx = ctx;
        this.memoria = memoria;
    }

    public void llamada(Llamada3D lam) {
        if (!lam.isUsuario()) {
            nativa(lam);
            return;
        }
        int retId = ctx.retIds().getOrDefault(lam, 0);
        int frame = Math.max(0, lam.getCallerFrameSize());
        List<MemoryAccess> args = lam.getArgumentos();
        memoria.guardarInmediatoSlot(frame, retId);
        for (int i = 0; i < args.size(); i++) {
            int slot;
            if (lam.isTieneSelf() && i == 0) {
                slot = 2;
            } else {
                slot = 3 + i - (lam.isTieneSelf() ? 1 : 0);
            }
            memoria.cargarEn(args.get(i), "ft0");
            memoria.guardarSlot(frame + slot);
        }
        if (frame != 0) {
            ctx.emitir("    addi s3, s3, " + frame);
        }
        ctx.emitir("    jal ra, " + ctx.etiquetaFuncion(lam.getFuncion()));
        ctx.emitir("Lret_" + retId + ":");
        if (lam.getResult() != null) {
            cargarRetorno(frame, "ft4");
        }
        if (frame != 0) {
            ctx.emitir("    addi s3, s3, -" + frame);
        }
        if (lam.getResult() != null) {
            ctx.emitir("    fmv.d ft0, ft4");
            memoria.guardar(lam.getResult());
        }
    }

    public void virtual(LlamadaVirtual3D virt) {
        int retId = ctx.retIdsVirt().getOrDefault(virt, 0);
        int frame = Math.max(0, virt.getCallerFrameSize());
        memoria.cargarEn(virt.getReceptor(), "ft0");
        memoria.guardarSlot(frame + 2);
        memoria.guardarInmediatoSlot(frame, retId);
        List<MemoryAccess> args = virt.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            memoria.cargarEn(args.get(i), "ft0");
            memoria.guardarSlot(frame + 3 + i);
        }
        if (frame != 0) {
            ctx.emitir("    addi s3, s3, " + frame);
        }
        ctx.emitir("    addi t0, s3, 2");
        ctx.emitir("    slli t0, t0, 3");
        ctx.emitir("    add t0, s0, t0");
        ctx.emitir("    fld ft0, 0(t0)");
        ctx.emitir("    fcvt.w.d t1, ft0, rtz");
        ctx.emitir("    slli t1, t1, 3");
        ctx.emitir("    add t1, s1, t1");
        ctx.emitir("    fld ft0, 0(t1)");
        ctx.emitir("    fcvt.w.d t1, ft0, rtz");
        boolean hay = false;
        for (Map.Entry<Integer, String> e : virt.tablaDespacho().entrySet()) {
            ctx.emitir("    li t0, " + e.getKey());
            ctx.emitir("    beq t1, t0, " + e.getValue());
            hay = true;
        }
        if (!hay) {
            ctx.error("tabla de despacho vacia para " + virt.getMetodo());
        }
        ctx.emitir("    jal ra, rt_error_despacho");
        ctx.emitir("Lret_" + retId + ":");
        if (virt.getDestino() != null) {
            cargarRetorno(frame, "ft4");
        }
        if (frame != 0) {
            ctx.emitir("    addi s3, s3, -" + frame);
        }
        if (virt.getDestino() != null) {
            ctx.emitir("    fmv.d ft0, ft4");
            memoria.guardar(virt.getDestino());
        }
    }

    private void nativa(Llamada3D lam) {
        String nombre = lam.getFuncion() == null ? "" : lam.getFuncion();
        List<MemoryAccess> args = lam.getArgumentos();
        switch (nombre) {
            case "strd" -> {
                if (!args.isEmpty()) {
                    memoria.cargarEn(args.get(0), "ft0");
                }
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_strd");
                resultado(lam);
            }
            case "strn" -> {
                if (!args.isEmpty()) {
                    memoria.cargarEn(args.get(0), "fa0");
                }
                ctx.emitir("    jal ra, rt_strn");
                resultado(lam);
            }
            case "strc" -> {
                if (!args.isEmpty()) {
                    memoria.cargarEn(args.get(0), "ft0");
                }
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_strc");
                resultado(lam);
            }
            case "conc" -> {
                memoria.cargarEn(args.size() > 0 ? args.get(0) : null, "ft0");
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                memoria.cargarEn(args.size() > 1 ? args.get(1) : null, "ft0");
                ctx.emitir("    fcvt.w.d a1, ft0, rtz");
                ctx.emitir("    jal ra, rt_conc");
                resultado(lam);
            }
            case "__print_int" -> {
                memoria.cargarEn(args.isEmpty() ? null : args.get(0), "ft0");
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_print_int");
            }
            case "__print_char" -> {
                memoria.cargarEn(args.isEmpty() ? null : args.get(0), "ft0");
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_print_char");
            }
            case "__print_double" -> {
                memoria.cargarEn(args.isEmpty() ? null : args.get(0), "fa0");
                ctx.emitir("    jal ra, rt_print_g");
            }
            case "__print_bool" -> {
                memoria.cargarEn(args.isEmpty() ? null : args.get(0), "fa0");
                ctx.emitir("    jal ra, rt_print_bool");
            }
            case "__print_pointer" -> {
                memoria.cargarEn(args.isEmpty() ? null : args.get(0), "ft0");
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_print_ptr");
            }
            default -> {
                if (lam.getResult() != null) {
                    memoria.guardarInmediato(lam.getResult(), 0);
                }
            }
        }
    }

    private void resultado(Llamada3D lam) {
        if (lam.getResult() != null) {
            ctx.emitir("    fcvt.d.w ft0, a0");
            memoria.guardar(lam.getResult());
        }
    }

    private void cargarRetorno(int frame, String fdest) {
        if (frame == 0) {
            ctx.emitir("    addi t0, s3, 1");
        } else {
            ctx.emitir("    addi t0, s3, " + (frame + 1));
        }
        ctx.emitir("    slli t0, t0, 3");
        ctx.emitir("    add t0, s0, t0");
        ctx.emitir("    fld " + fdest + ", 0(t0)");
    }
}
