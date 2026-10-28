package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.access.CampoAccess;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.access.TemporalAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;

import java.util.ArrayList;
import java.util.List;

public final class GeneradorCuerpo {
    private final ContextoRiscV ctx;
    private final RiscVEmisor emisor;

    private record Bloque(InicioFuncion3D inicio, List<Cuarteta> cuerpo) {
    }

    public GeneradorCuerpo(ContextoRiscV ctx, RiscVEmisor emisor) {
        this.ctx = ctx;
        this.emisor = emisor;
    }

    public void generar(List<Cuarteta> todas) {
        List<Cuarteta> topLevel = new ArrayList<>();
        List<Bloque> funciones = new ArrayList<>();
        int i = 0;
        while (i < todas.size()) {
            Cuarteta actual = todas.get(i);
            if (actual instanceof InicioFuncion3D inicio) {
                int fin = i + 1;
                while (fin < todas.size() && !(todas.get(fin) instanceof FinFuncion3D)) {
                    fin++;
                }
                int hasta = Math.min(fin, todas.size());
                funciones.add(new Bloque(inicio, new ArrayList<>(todas.subList(i + 1, hasta))));
                i = fin < todas.size() ? fin + 1 : todas.size();
            } else {
                topLevel.add(actual);
                i++;
            }
        }

        MarcoRiscV mMain = MarcoRiscV.desde("main", "");
        ctx.setMarcoActual(mMain);
        ctx.setCuerpoActual(topLevel);
        analizar(topLevel, mMain);

        ctx.emitir("main:");
        ctx.emitir("    la s1, Heap");
        ctx.emitir("    la s2, Str_libre");
        prologo(mMain);
        for (Cuarteta c : topLevel) {
            emisor.emitir(c);
        }
        ctx.emitir("main_fin:");
        epilogo(mMain, true);

        for (Bloque b : funciones) {
            MarcoRiscV m = MarcoRiscV.desde(b.inicio().getNombre(), b.inicio().getDescripcion());
            ctx.setMarcoActual(m);
            ctx.setCuerpoActual(b.cuerpo());
            analizar(b.cuerpo(), m);
            ctx.registrarFuncion(m.nombre);

            String et = ctx.etiquetaFuncion(m.nombre);
            ctx.emitir(et + ":");
            prologo(m);
            regarParams(m);
            for (Cuarteta c : b.cuerpo()) {
                emisor.emitir(c);
            }
            ctx.emitir(et + "_fin:");
            epilogo(m, false);
        }
        ctx.setMarcoActual(null);
    }

    private void analizar(List<Cuarteta> cuerpo, MarcoRiscV m) {
        for (Cuarteta c : cuerpo) {
            for (MemoryAccess op : ctx.operandosDe(c)) {
                observar(op, m);
            }
            if (c instanceof DeclararArreglo3D arr) {
                NameAccess na = ctx.accesoDeNombre(arr.getNombre());
                if (na != null && !na.isGlobal() && !na.isAutoreferencia()
                        && na.getOffset() > m.maxSlot) {
                    m.maxSlot = na.getOffset();
                }
            }
            if (c instanceof Llamada3D lam) {
                List<TipoRV> tipos = new ArrayList<>();
                for (MemoryAccess a : lam.getArgumentos()) tipos.add(ctx.getTipoRV(a));
                m.salientes = Math.max(m.salientes,
                        ConvencionLlamada.salientes(ConvencionLlamada.asignar(tipos), tipos));
            }
            if (c instanceof LlamadaVirtual3D virt) {
                List<TipoRV> tipos = new ArrayList<>();
                tipos.add(TipoRV.REF);
                for (MemoryAccess a : virt.getArgumentos()) tipos.add(ctx.getTipoRV(a));
                m.salientes = Math.max(m.salientes,
                        ConvencionLlamada.salientes(ConvencionLlamada.asignar(tipos), tipos));
            }
        }
    }

    private void observar(MemoryAccess op, MarcoRiscV m) {
        if (op == null) return;
        if (op instanceof NameAccess na) {
            if (na.isAutoreferencia() || na.isGlobal()) return;
            if (na.getOffset() > m.maxSlot) m.maxSlot = na.getOffset();
            return;
        }
        if (op instanceof TemporalAccess ta) {
            if (ta.getOffset() > m.maxSlot) m.maxSlot = ta.getOffset();
            return;
        }
        if (op instanceof CampoAccess ca) {
            observar(ca.getBase(), m);
            return;
        }
        if (op instanceof IndiceAccess ia) {
            observar(ia.getBase(), m);
            observar(ia.getIndice(), m);
        }
    }

    private void regarParams(MarcoRiscV m) {
        List<TipoRV> tipos = new ArrayList<>();
        for (MarcoRiscV.Param p : m.params) tipos.add(p.tipo);
        List<ConvencionLlamada.UbicacionArg> ubicaciones = ConvencionLlamada.asignar(tipos);
        for (int i = 0; i < m.params.size(); i++) {
            MarcoRiscV.Param p = m.params.get(i);
            ConvencionLlamada.UbicacionArg u = ubicaciones.get(i);
            int destino = ctx.slotByte(p.slot);
            switch (u.donde()) {
                case REG_ENTERO -> AccesoMemoria.emitir(ctx, "sw", "a" + u.indiceReg(), "s0", destino);
                case REG_DOBLE -> AccesoMemoria.emitir(ctx, "fsd", "fa" + u.indiceReg(), "s0", destino);
                case PILA -> {
                    int origen = 4 + u.offsetPila();
                    if (p.tipo == TipoRV.DOBLE) {
                        AccesoMemoria.emitir(ctx, "fld", "ft0", "s0", origen);
                        AccesoMemoria.emitir(ctx, "fsd", "ft0", "s0", destino);
                    } else {
                        AccesoMemoria.emitir(ctx, "lw", "t0", "s0", origen);
                        AccesoMemoria.emitir(ctx, "sw", "t0", "s0", destino);
                    }
                }
            }
        }
    }

    private void prologo(MarcoRiscV m) {
        int N = m.tamano();
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

    private void epilogo(MarcoRiscV m, boolean esMain) {
        int N = m.tamano();
        ctx.emitir("    lw ra, -4(s0)");
        ctx.emitir("    lw s0, 0(s0)");
        if (N <= 2047) {
            ctx.emitir("    addi sp, sp, " + N);
        } else {
            ctx.emitir("    li t6, " + N);
            ctx.emitir("    add sp, sp, t6");
        }
        if (esMain) {
            ctx.emitir("    li a7, 10");
            ctx.emitir("    ecall");
        } else {
            ctx.emitir("    ret");
        }
    }
}
