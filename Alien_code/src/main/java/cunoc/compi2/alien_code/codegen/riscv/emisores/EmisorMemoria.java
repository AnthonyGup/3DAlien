package cunoc.compi2.alien_code.codegen.riscv.emisores;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.CampoAccess;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.access.TemporalAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.codegen.riscv.AccesoMemoria;
import cunoc.compi2.alien_code.codegen.riscv.ContextoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.MarcoRiscV;
import cunoc.compi2.alien_code.codegen.riscv.OffsetsBytes;
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public final class EmisorMemoria {
    private final ContextoRiscV ctx;

    public EmisorMemoria(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    public void asignar(Asignacion3D asg) {
        MemoryAccess destino = asg.getResult();
        MemoryAccess valor = asg.getOperand1();
        if (valor == null) {
            guardarInmediato(destino, 0);
            return;
        }
        if (valor instanceof Literal3D lit) {
            TipoRV tDest = ctx.getTipoRV(destino);
            TipoRV tSrc = ctx.getTipoRV(lit);
            // Conversión si necesario
            cargarLiteralEn(lit, "ft0", tSrc);
            if (tDest != tSrc) convertir("ft0", tSrc, tDest);
            guardar(destino, tDest);
            return;
        }
        TipoRV t = ctx.getTipoRV(valor);
        cargarEn(valor, t);
        guardar(valor, t);
    }

    public void declararArreglo(DeclararArreglo3D arreglo) {
        Symbol simbolo = ctx.simbolo(arreglo.getNombre());
        if (simbolo == null || simbolo.getOffset() < 0) {
            ctx.error("arreglo sin offset: " + arreglo.getNombre());
            return;
        }
        long total = tamanoArreglo(arreglo.getDims());
        // malloc en heap: mv t0, s1; addi s1, s1, total*8; sw t0, offset(s0)
        ctx.emitir("    mv t0, s1");
        int bytes = (int) Math.max(8, total * 8);
        if (bytes <= 2047) {
            ctx.emitir("    addi s1, s1, " + bytes);
        } else {
            ctx.emitir("    li t6, " + bytes);
            ctx.emitir("    add s1, s1, t6");
        }
        // Guardar la dirección base (t0) en la variable
        int off = ctx.getOffsetGlobal(arreglo.getNombre());
        if (off >= 0) {
            AccesoMemoria.emitir(ctx, "sw", "t0", "s0", off);
        } else {
            // local
            // Para locales, buscar el NameAccess correspondiente
            off = ctx.getOffsetGlobal(arreglo.getNombre()); // fallback
            AccesoMemoria.emitir(ctx, "sw", "t0", "s0", off);
        }
        // Poner id de clase? No, los arreglos no tienen cabecera de clase
    }

    public void cargarEn(MemoryAccess op, TipoRV t) {
        String defReg = (t == TipoRV.DOBLE) ? "ft0" : "t0";
        cargarEn(op, defReg, t);
    }

    public void cargarEn(MemoryAccess op, String fdest, TipoRV t) {
        if (op == null) {
            cero(fdest, t);
            return;
        }
        if (op instanceof Literal3D lit) {
            cargarLiteralEn(lit, fdest, t);
            return;
        }
        if (op instanceof NameAccess na) {
            cargarNombre(na, fdest, t);
            return;
        }
        if (op instanceof TemporalAccess ta) {
            cargarTemporal(ta, fdest, t);
            return;
        }
        if (op instanceof CampoAccess campo) {
            cargarCampo(campo, fdest, t);
            return;
        }
        if (op instanceof IndiceAccess indice) {
            cargarIndice(indice, fdest, t);
            return;
        }
        if (op instanceof LabelAccess) {
            ctx.error("no se puede cargar una etiqueta");
            cero(fdest, t);
            return;
        }
        ctx.error("operando no soportado para carga: " + op.getClass().getSimpleName());
        cero(fdest, t);
    }

    public void guardar(MemoryAccess destino, TipoRV t) {
        // ft0 ya tiene el valor en el registro correcto según tipo
        String srcReg = (t == TipoRV.DOBLE) ? "ft0" : "t0";
        if (destino instanceof NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                // self está en a0 (entrante) o guardado en -8(s0)
                MarcoRiscV marco = ctx.getMarcoActual();
                if (marco != null) {
                    int off = marco.getOffsetParam("self");
                    if (off >= -2048 && off <= 2047) {
                        ctx.emitir("    " + t.guarda + " " + srcReg + ", " + off + "(s0)");
                    } else {
                        AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
                    }
                } else {
                    // top-level: no hay self, error
                    ctx.error("self referenciado fuera de metodo");
                }
                return;
            }
            if (na.isGlobal()) {
                int off = ctx.getOffsetGlobal(na.getNombre());
                if (off >= 0) {
                    if (off <= 2047) ctx.emitir("    " + t.guarda + " " + srcReg + ", " + off + "(s0)");
                    else AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
                } else {
                    ctx.emitir("    la t0, Globals");
                    AccesoMemoria.emitir(ctx, t.guarda, srcReg, "t0", off);
                }
                return;
            }
            // local
            int off = ctx.getOffset(na);
            AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
            return;
        }
        if (destino instanceof TemporalAccess ta) {
            int off = ctx.getOffset(ta);
            AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s0", off);
            return;
        }
        if (destino instanceof CampoAccess campo) {
            guardarCampo(campo, srcReg, t);
            return;
        }
        if (destino instanceof IndiceAccess indice) {
            guardarIndice(indice, srcReg, t);
            return;
        }
        ctx.error("destino no soportado para guardar: " + destino.getClass().getSimpleName());
    }

    public void guardarInmediato(MemoryAccess destino, long valor) {
        TipoRV t = ctx.getTipoRV(destino);
        if (t == TipoRV.DOBLE) {
            ctx.emitir("    li t0, " + valor);
            ctx.emitir("    fcvt.d.w ft0, t0");
        } else {
            ctx.emitir("    li t0, " + valor);
        }
        guardar(destino, t);
    }

    private void cargarLiteralEn(Literal3D lit, String fdest, TipoRV t) {
        String txt = lit.getTexto() == null ? "0" : lit.getTexto().trim();
        if (txt.startsWith("\"")) {
            String et = ctx.etiquetaCadena(txt);
            ctx.emitir("    la t0, " + et);
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + fdest + ", t0");
            else { /* REF se carga como dirección */ }
            return;
        }
        if ("NULL".equals(txt)) {
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + fdest + ", zero");
            else ctx.emitir("    li " + fdest + ", 0");
            return;
        }
        if ("H".equals(txt)) {
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + fdest + ", s1");
            else ctx.emitir("    mv " + fdest + ", s1");
            return;
        }
        if ("S".equals(txt)) {
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + fdest + ", s2");
            else ctx.emitir("    mv " + fdest + ", s2");
            return;
        }
        // Entero o double literal
        if (txt.matches("^[+-]?\\d+$")) {
            ctx.emitir("    li t0, " + txt);
            if (t == TipoRV.DOBLE) ctx.emitir("    fcvt.d.w " + fdest + ", t0");
            else { ctx.emitir("    mv " + fdest + ", t0"); }
            return;
        }
        if (txt.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            String et = ctx.etiquetaConstante(txt);
            ctx.emitir("    la t0, " + et);
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        ctx.error("literal no reconocido: " + txt);
        cero(fdest, t);
    }

    private void cargarNombre(NameAccess na, String fdest, TipoRV t) {
        if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
            // self en -8(s0)
            AccesoMemoria.emitir(ctx, t.carga, fdest, "s0", -8);
            return;
        }
        if (na.isGlobal()) {
            int off = ctx.getOffsetGlobal(na.getNombre());
            if (off >= 0) {
                if (off <= 2047) ctx.emitir("    " + t.carga + " " + fdest + ", " + off + "(s0)");
                else {
                    ctx.emitir("    la t0, Globals");
                    AccesoMemoria.emitir(ctx, t.carga, fdest, "t0", off);
                }
            } else {
                ctx.emitir("    la t0, Globals");
                AccesoMemoria.emitir(ctx, t.carga, fdest, "t0", off);
            }
            return;
        }
        // local
        int off = ctx.getOffset(na);
        AccesoMemoria.emitir(ctx, t.carga, fdest, "s0", off);
    }

    private void cargarTemporal(TemporalAccess ta, String fdest, TipoRV t) {
        int off = ctx.getOffset(ta);
        AccesoMemoria.emitir(ctx, t.carga, fdest, "s0", off);
    }

    private void cargarCampo(CampoAccess campo, String fdest, TipoRV t) {
        // base en t0 -> add offset -> load
        cargarEn(campo.getBase(), "t0", TipoRV.REF);
        int off = ctx.getOffset(campo);
        if (off != 0) ctx.emitir("    addi t0, t0, " + off);
        AccesoMemoria.emitir(ctx, t.carga, fdest, "s1", 0);
    }

    private void cargarIndice(IndiceAccess ind, String fdest, TipoRV t) {
        // base + indice
        cargarEn(ind.getBase(), "t0", TipoRV.REF);
        cargarEn(ind.getIndice(), "t1", TipoRV.ENTERO);
        ctx.emitir("    add t0, t0, t1");
        AccesoMemoria.emitir(ctx, t.carga, fdest, "s1", 0);
    }

    private void guardarCampo(CampoAccess campo, String srcReg, TipoRV t) {
        cargarEn(campo.getBase(), "t0", TipoRV.REF);
        int off = ctx.getOffset(campo);
        if (off != 0) ctx.emitir("    addi t0, t0, " + off);
        AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s1", 0);
    }

    private void guardarIndice(IndiceAccess ind, String srcReg, TipoRV t) {
        cargarEn(ind.getBase(), "t0", TipoRV.REF);
        cargarEn(ind.getIndice(), "t1", TipoRV.ENTERO);
        ctx.emitir("    add t0, t0, t1");
        AccesoMemoria.emitir(ctx, t.guarda, srcReg, "s1", 0);
    }

    private void cero(String fdest, TipoRV t) {
        // Si el registro es de punto flotante (empieza con 'f'), usar fcvt.d.w
        // Si es entero, usar li. Esto evita "li ft0, 0" que es inválido.
        boolean esFlotante = fdest != null && fdest.startsWith("f");
        if (esFlotante || t == TipoRV.DOBLE) {
            ctx.emitir("    fcvt.d.w " + fdest + ", zero");
        } else {
            ctx.emitir("    li " + fdest + ", 0");
        }
    }

    private void convertir(String reg, TipoRV de, TipoRV a) {
        if (de == a) return;
        if (de.esEntero() && a == TipoRV.DOBLE) {
            ctx.emitir("    fcvt.d.w " + reg + ", " + reg);
        } else if (de == TipoRV.DOBLE && a.esEntero()) {
            ctx.emitir("    fcvt.w.d " + reg + ", " + reg + ", rtz");
        }
    }

    private long tamanoArreglo(String dims) {
        if (dims == null || dims.isBlank()) return 1;
        long total = 1;
        for (String parte : dims.split(",")) {
            String limpio = parte.trim();
            if (limpio.isEmpty()) continue;
            try { total *= Math.max(1, Long.parseLong(limpio)); }
            catch (NumberFormatException e) { return 1; }
        }
        return total;
    }
}