package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;
import cunoc.compi2.alien_code.codegen.riscv.AccesoMemoria;
import cunoc.compi2.alien_code.codegen.riscv.ConvencionLlamada;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;
import cunoc.compi2.alien_code.ir.ClaseLayout;

import java.util.ArrayList;
import java.util.List;

public final class EmisorLlamadas {
    private final ContextoRiscV ctx;
    private final EmisorMemoria memoria;
    private final EmisorAritmetica aritmetica;

    public EmisorLlamadas(ContextoRiscV ctx, EmisorMemoria memoria) {
        this.ctx = ctx;
        this.memoria = memoria;
        this.aritmetica = new EmisorAritmetica(ctx, memoria);
    }

    public void llamada(Llamada3D lam) {
        String fn = lam.getFuncion();
        if (nativa(lam, fn)) return;
        List<MemoryAccess> args = lam.getArgumentos();
        List<TipoRV> tipos = new ArrayList<>();
        for (MemoryAccess a : args) tipos.add(ctx.getTipoRV(a));
        List<ConvencionLlamada.UbicacionArg> ubicaciones = ConvencionLlamada.asignar(tipos);
        for (int i = 0; i < args.size(); i++) {
            cargarArgumento(args.get(i), tipos.get(i), ubicaciones.get(i));
        }
        ctx.registrarLlamadaUsuario(fn);
        ctx.emitir("    jal ra, " + ctx.etiquetaFuncion(fn));
        guardarRetorno(lam.getResult());
    }

    private boolean nativa(Llamada3D lam, String fn) {
        List<MemoryAccess> args = lam.getArgumentos();
        switch (fn == null ? "" : fn) {
            case "conc" -> {
                aritmetica.emitirNativa(fn, args);
            }
            case "strd" -> {
                aritmetica.emitirNativa(fn, args);
            }
            case "strn" -> {
                aritmetica.emitirNativa(fn, args);
            }
            case "strc" -> {
                aritmetica.emitirNativa(fn, args);
            }
            default -> { return false; }
        }
        guardarRetorno(lam.getResult());
        return true;
    }

    public void virtual(LlamadaVirtual3D virt) {
        List<TipoRV> tipos = new ArrayList<>();
        tipos.add(TipoRV.REF);
        for (MemoryAccess a : virt.getArgumentos()) tipos.add(ctx.getTipoRV(a));
        List<ConvencionLlamada.UbicacionArg> ubicaciones = ConvencionLlamada.asignar(tipos);

        memoria.cargarEntero(virt.getReceptor(), "a" + ubicaciones.get(0).indiceReg());
        for (int i = 0; i < virt.getArgumentos().size(); i++) {
            cargarArgumento(virt.getArgumentos().get(i), tipos.get(i + 1), ubicaciones.get(i + 1));
        }

        int k = ctx.siguienteSitioVirtual();
        ctx.emitir("    lw t1, 0(a0)");
        int n = 0;
        for (ClaseLayout.ImplMetodo impl : virt.getCandidatos()) {
            ctx.emitir("    li t2, " + impl.claseId);
            ctx.emitir("    beq t1, t2, Lv" + k + "_c" + n);
            n++;
        }
        ctx.emitirErrorInline("error_despacho");
        n = 0;
        for (ClaseLayout.ImplMetodo impl : virt.getCandidatos()) {
            ctx.emitir("Lv" + k + "_c" + n + ":");
            ctx.emitir("    jal ra, " + impl.etiqueta);
            ctx.emitir("    j Lv" + k + "_fin");
            n++;
        }
        ctx.emitir("Lv" + k + "_fin:");
        guardarRetorno(virt.getDestino());
    }

    private void cargarArgumento(MemoryAccess arg, TipoRV tipo, ConvencionLlamada.UbicacionArg u) {
        switch (u.donde()) {
            case REG_ENTERO -> memoria.cargarEntero(arg, "a" + u.indiceReg());
            case REG_DOBLE -> memoria.cargarDouble(arg, "fa" + u.indiceReg());
            case PILA -> {
                if (tipo == TipoRV.DOBLE) {
                    memoria.cargarDouble(arg, "ft0");
                    AccesoMemoria.emitir(ctx, "fsd", "ft0", "sp", u.offsetPila());
                } else {
                    memoria.cargarEntero(arg, "t0");
                    AccesoMemoria.emitir(ctx, "sw", "t0", "sp", u.offsetPila());
                }
            }
        }
    }

    private void guardarRetorno(MemoryAccess destino) {
        if (destino == null) return;
        if (ctx.getTipoRV(destino) == TipoRV.DOBLE) {
            memoria.guardarDouble(destino, "fa0");
        } else {
            memoria.guardarEntero(destino, "a0");
        }
    }
}
