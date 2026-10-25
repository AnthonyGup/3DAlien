package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.MarcoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

import java.util.List;

public final class EmisorLlamadas {
    private final ContextoRiscV ctx;

    public EmisorLlamadas(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void llamada(Llamada3D lam) {
        if (!lam.isUsuario()) {
            nativa(lam);
            return;
        }
        String fn = lam.getFuncion();
        int retId = ctx.retIds().getOrDefault(lam, 0);
        List<MemoryAccess> args = lam.getArgumentos();
        MarcoRiscV caller = ctx.getMarcoActual();

        // Evaluar argumentos en registros correctos
        int intArg = 0;
        int dblArg = 0;
        for (MemoryAccess arg : args) {
            TipoRV t = ctx.getTipoRV(arg);
            if (t == TipoRV.DOBLE) {
                if (dblArg < 8) {
                    cargarComo(arg, "fa" + dblArg, t);
                    dblArg++;
                } else {
                    ctx.error("mas de 8 argumentos double no soportado");
                }
            } else {
                if (intArg < 8) {
                    cargarComo(arg, "a" + intArg, t);
                    intArg++;
                } else {
                    // 9.o en adelante: en pila del caller (sp)
                    int off = caller.getSalientes(); // ya calculado en marco
                    // No implementado aún: guardar en 0(sp), 4(sp)...
                    ctx.emitir("    # FIXME: argumento " + intArg + " en pila");
                    intArg++;
                }
            }
        }

        // Llamar
        ctx.emitir("    jal ra, " + ctx.etiquetaFuncion(fn));

        // Guardar retorno INMEDIATAMENTE
        if (lam.getResult() != null) {
            TipoRV tRes = ctx.getTipoRV(lam.getResult());
            if (tRes == TipoRV.DOBLE) {
                ctx.emitir("    fsd fa0, " + offset(lam.getResult()) + "(s0)");
            } else {
                ctx.emitir("    sw a0, " + offset(lam.getResult()) + "(s0)");
            }
        }
    }

    public void virtual(LlamadaVirtual3D virt) {
        MemoryAccess receptor = virt.getReceptor();
        List<MemoryAccess> args = virt.getArgumentos();
        MemoryAccess destino = virt.getDestino();

        // Cargar receptor en a0
        cargarComo(receptor, "a0", ctx.getTipoRV(receptor));

        // Argumentos: a1-a7 para enteros, fa1-fa7 para doubles
        int intArg = 1;
        int dblArg = 0;
        for (MemoryAccess arg : args) {
            TipoRV t = ctx.getTipoRV(arg);
            if (t == TipoRV.DOBLE) {
                if (dblArg < 8) {
                    cargarComo(arg, "fa" + dblArg, t);
                    dblArg++;
                }
            } else {
                if (intArg < 8) {
                    cargarComo(arg, "a" + intArg, t);
                    intArg++;
                }
            }
        }

        // Despacho: leer id de clase desde offset 0 del objeto
        ctx.emitir("    lw t1, 0(a0)");
        boolean hay = false;
        for (var e : virt.tablaDespacho().entrySet()) {
            ctx.emitir("    li t2, " + e.getKey());
            ctx.emitir("    beq t1, t2, " + e.getValue());
            hay = true;
        }
        if (!hay) {
            ctx.error("tabla de despacho vacia para " + virt.getMetodo());
            ctx.emitir("    jal ra, rt_error_despacho");
        } else {
            // Etiquetas de continuación
            ctx.emitir("    j Lvirt_fin_" + ctx.retIdsVirt().get(virt));
        }

        // Cada caso termina en Lvirt_fin
        ctx.emitir("Lvirt_fin_" + ctx.retIdsVirt().get(virt) + ":");
        if (destino != null) {
            TipoRV tRes = ctx.getTipoRV(destino);
            if (tRes == TipoRV.DOBLE) {
                ctx.emitir("    fsd fa0, " + offset(destino) + "(s0)");
            } else {
                ctx.emitir("    sw a0, " + offset(destino) + "(s0)");
            }
        }
    }

    private void nativa(Llamada3D lam) {
        String nombre = lam.getFuncion() == null ? "" : lam.getFuncion();
        List<MemoryAccess> args = lam.getArgumentos();
        switch (nombre) {
            case "strd" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_strd");
                resultado(lam);
            }
            case "strn" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "fa0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_strn");
                resultado(lam);
            }
            case "strc" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_strc");
                resultado(lam);
            }
            case "conc" -> {
                cargarComo(args.size() > 0 ? args.get(0) : null, "a0", args.size() > 0 ? ctx.getTipoRV(args.get(0)) : TipoRV.REF);
                cargarComo(args.size() > 1 ? args.get(1) : null, "a1", args.size() > 1 ? ctx.getTipoRV(args.get(1)) : TipoRV.REF);
                ctx.emitir("    jal ra, rt_conc");
                resultado(lam);
            }
            case "__print_int" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_print_int");
            }
            case "__print_char" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_print_char");
            }
            case "__print_double" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "fa0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_print_g");
            }
            case "__print_bool" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_print_bool");
            }
            case "__print_pointer" -> {
                if (!args.isEmpty()) cargarComo(args.get(0), "a0", ctx.getTipoRV(args.get(0)));
                ctx.emitir("    jal ra, rt_print_ptr");
            }
            default -> {
                if (lam.getResult() != null) {
                    guardarInmediato(lam.getResult(), 0);
                }
            }
        }
    }

    private void resultado(Llamada3D lam) {
        if (lam.getResult() != null) {
            TipoRV t = ctx.getTipoRV(lam.getResult());
            if (t == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w ft0, a0");
            } else {
                ctx.emitir("    mv t0, a0");
            }
            guardar(lam.getResult(), t);
        }
    }

    private void cargarComo(MemoryAccess op, String reg, TipoRV t) {
        if (op == null) {
            boolean esFlotante = reg != null && reg.startsWith("f");
            if (esFlotante || t == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w " + reg + ", zero");
            } else {
                ctx.emitir("    li " + reg + ", 0");
            }
            return;
        }
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    fld " + reg + ", " + ctx.getOffset(op) + "(s0)");
        } else {
            ctx.emitir("    lw " + reg + ", " + ctx.getOffset(op) + "(s0)");
        }
    }

    private void guardar(MemoryAccess dest, TipoRV t) {
        String srcReg = (t == TipoRV.DOBLE) ? "ft0" : "t0";
        if (dest instanceof cunoc.compi2.alien_code.c3d.access.NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                int off = -8; // self en -8(s0)
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

    private void guardarInmediato(MemoryAccess dest, long val) {
        if (dest == null) return;
        TipoRV t = ctx.getTipoRV(dest);
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    li t0, " + val);
            ctx.emitir("    fcvt.d.w ft0, t0");
            guardar(dest, t);
        } else {
            ctx.emitir("    li t0, " + val);
            guardar(dest, t);
        }
    }

    private int offset(MemoryAccess op) {
        return ctx.getOffset(op);
    }
}