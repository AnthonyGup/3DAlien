package cunoc.compi2.alien_code.codegen.riscv.emisores;

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
import cunoc.compi2.alien_code.codegen.riscv.TipoRV;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmisorMemoria {
    private static final String[] POOL = {"t0", "t1", "t2", "t3", "t4", "t5"};

    private static final Pattern P_LOCAL =
            Pattern.compile("Stack\\[\\s*\\(int\\)\\s*\\(\\s*P\\s*\\+\\s*(\\d+)\\s*\\)\\s*\\]");
    private static final Pattern P_GLOBAL =
            Pattern.compile("Stack\\[\\s*\\(int\\)\\s*(\\d+)\\s*\\]");
    private static final Pattern P_HEAP_SELF =
            Pattern.compile("Heap\\[\\s*\\(int\\)\\s*\\(\\s*Stack\\[\\s*\\(int\\)\\s*\\(\\s*P\\s*\\+\\s*(\\d+)\\s*\\)\\s*\\]\\s*\\)\\s*\\+\\s*(\\d+)\\s*\\]");

    private final ContextoRiscV ctx;

    public EmisorMemoria(ContextoRiscV ctx) {
        this.ctx = ctx;
    }

    // ---------------------------------------------------------------- asignacion

    public void asignar(Asignacion3D asg) {
        MemoryAccess destino = asg.getResult();
        MemoryAccess valor = asg.getOperand1();
        if (destino == null) return;
        if (valor instanceof Literal3D lit) {
            String txt = lit.getTexto() == null ? "" : lit.getTexto().trim();
            if (txt.startsWith("malloc(")) {
                asignarMalloc(destino, txt);
                return;
            }
            if (txt.startsWith("{") || txt.startsWith("&(")
                    || (txt.startsWith("(") && txt.contains("[]){"))) {
                asignarLiteralCompuesto(destino, txt);
                return;
            }
        }
        if (ctx.getTipoRV(destino) == TipoRV.DOBLE) {
            cargarDouble(valor, "ft0");
            guardarDouble(destino, "ft0");
        } else {
            cargarEntero(valor, "t0");
            guardarEntero(destino, "t0");
        }
    }

    private void asignarMalloc(MemoryAccess destino, String txt) {
        String inner = txt.substring("malloc(".length());
        inner = inner.substring(0, inner.length() - 1).trim();

        int corte = inner.indexOf(" * sizeof(");
        String cuenta = corte >= 0 ? inner.substring(0, corte).trim() : inner;

        if (corte < 0 && cuenta.startsWith("sizeof(")) {
            String tipo = cuenta.startsWith("sizeof(struct ")
                    ? cuenta.substring("sizeof(struct ".length(), cuenta.length() - 1).trim()
                    : "";
            int slots = ctx.layoutsRiscV().slots(tipo);
            int id = ctx.layoutsRiscV().idClase(tipo);
            reservar(destino, slots);
            if (id >= 0) {
                ctx.emitir("    li t1, " + id);
                ctx.emitir("    sw t1, 0(t0)");
            }
            return;
        }
        if (cuenta.matches("^\\d+$")) {
            reservar(destino, Integer.parseInt(cuenta));
            return;
        }
        avanzarHeapCuenta(cuenta);
        guardarEntero(destino, "t0");
    }

    private void reservar(MemoryAccess destino, int slots) {
        avanzarHeap(Math.max(1, slots) * 8);
        guardarEntero(destino, "t0");
    }

    private void avanzarHeap(int bytes) {
        ctx.emitir("    mv t0, s1");
        if (bytes <= 2047) {
            ctx.emitir("    addi s1, s1, " + bytes);
        } else {
            ctx.emitir("    li t1, " + bytes);
            ctx.emitir("    add s1, s1, t1");
        }
    }

    private void avanzarHeapCuenta(String cuenta) {
        evaluarCuenta(cuenta, "t1");
        ctx.emitir("    slli t1, t1, 3");
        ctx.emitir("    mv t0, s1");
        ctx.emitir("    add s1, s1, t1");
    }

    private void evaluarCuenta(String expr, String reg) {
        if (expr.matches("^\\d+$")) {
            ctx.emitir("    li " + reg + ", " + expr);
            return;
        }
        Matcher m = P_LOCAL.matcher(expr);
        if (m.matches()) {
            AccesoMemoria.emitir(ctx, "lw", reg, "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
            return;
        }
        m = P_GLOBAL.matcher(expr);
        if (m.matches()) {
            String base = "t2".equals(reg) ? "t3" : "t2";
            ctx.emitir("    la " + base + ", Globals");
            AccesoMemoria.emitir(ctx, "lw", reg, base, ctx.globalByte(Integer.parseInt(m.group(1))));
            return;
        }
        m = P_HEAP_SELF.matcher(expr);
        if (m.matches()) {
            AccesoMemoria.emitir(ctx, "lw", reg, "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
            sumarOffset(reg, Integer.parseInt(m.group(2)) * 8);
            ctx.emitir("    lw " + reg + ", 0(" + reg + ")");
            return;
        }
        ctx.error("expresion de malloc no reconocida: " + expr);
        ctx.emitir("    li " + reg + ", 1");
    }

    public void cargarTexto(String texto, String reg, int depth) {
        String t = texto == null ? "0" : texto.trim();
        if (t.isEmpty() || "NULL".equals(t) || "0".equals(t)) {
            ctx.emitir("    li " + reg + ", 0");
            return;
        }
        if (t.matches("^\\d+$")) {
            ctx.emitir("    li " + reg + ", " + t);
            return;
        }
        if ("self".equals(t) || "H".equals(t) || "S".equals(t)) {
            Literal3D lit = new Literal3D(t, null);
            literalEntero(lit, reg);
            return;
        }
        if (t.matches("^'[^']+'$")) {
            ctx.emitir("    li " + reg + ", " + (int) t.charAt(1));
            return;
        }
        Matcher m = P_LOCAL.matcher(t);
        if (m.matches()) {
            AccesoMemoria.emitir(ctx, "lw", reg, "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
            return;
        }
        m = P_GLOBAL.matcher(t);
        if (m.matches()) {
            String base = "t2".equals(reg) ? "t3" : "t2";
            ctx.emitir("    la " + base + ", Globals");
            AccesoMemoria.emitir(ctx, "lw", reg, base, ctx.globalByte(Integer.parseInt(m.group(1))));
            return;
        }
        m = P_HEAP_SELF.matcher(t);
        if (m.matches()) {
            AccesoMemoria.emitir(ctx, "lw", reg, "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
            sumarOffset(reg, Integer.parseInt(m.group(2)) * 8);
            ctx.emitir("    lw " + reg + ", 0(" + reg + ")");
            return;
        }
        ctx.error("texto de operando no reconocido: " + t);
        ctx.emitir("    li " + reg + ", 0");
    }

    // ------------------------------------------------- literales de struct/arreglo

    private void asignarLiteralCompuesto(MemoryAccess destino, String txt) {
        String tipoNombre = null;
        TipoRV tipoElemento = TipoRV.ENTERO;
        if (txt.startsWith("&(struct ")) {
            tipoNombre = txt.substring(9, txt.indexOf(')')).trim();
        } else if (txt.startsWith("(")) {
            String palabra = txt.substring(1, txt.indexOf("[]){"));
            tipoElemento = TipoRV.desdeString(palabra);
        } else {
            tipoNombre = destino.getTipoNombre();
        }

        int abre = txt.indexOf('{');
        int cierra = txt.lastIndexOf('}');
        List<String> valores = partirPorComas(txt.substring(abre + 1, cierra));

        reservar(destino, Math.max(1, valores.size()));
        llenarLiteral(tipoNombre, tipoElemento, valores, 0);
    }

    private void llenarLiteral(String tipoNombre, TipoRV tipoElemento, List<String> valores, int depth) {
        String base = POOL[depth];
        String vreg = POOL[depth + 1];
        for (int i = 0; i < valores.size(); i++) {
            String v = valores.get(i).trim();
            if (v.isEmpty() || "0".equals(v) || "NULL".equals(v)) continue;
            TipoRV tc = tipoNombre != null ? ctx.campoTipoDe(tipoNombre, i) : tipoElemento;
            escribirValor(v, tc, base, vreg, i * 8, depth);
        }
    }

    private void escribirValor(String v, TipoRV tc, String base, String vreg, int off, int depth) {
        if (v.matches("^'[^']+'$")) {
            ctx.emitir("    li " + vreg + ", " + (int) v.charAt(1));
            if (tc == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w ft0, " + vreg);
                guardarEnBase(base, "ft0", true, off);
            } else {
                guardarEnBase(base, vreg, false, off);
            }
            return;
        }
        if (v.matches("^[+-]?\\d+$")) {
            ctx.emitir("    li " + vreg + ", " + v);
            if (tc == TipoRV.DOBLE) {
                ctx.emitir("    fcvt.d.w ft0, " + vreg);
                guardarEnBase(base, "ft0", true, off);
            } else {
                guardarEnBase(base, vreg, false, off);
            }
            return;
        }
        if (v.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            String et = ctx.registrarConstante(v);
            ctx.emitir("    la " + vreg + ", " + et);
            ctx.emitir("    fld ft0, 0(" + vreg + ")");
            if (tc == TipoRV.DOBLE) {
                guardarEnBase(base, "ft0", true, off);
            } else {
                ctx.emitir("    fcvt.w.d " + vreg + ", ft0, rtz");
                guardarEnBase(base, vreg, false, off);
            }
            return;
        }
        if (v.startsWith("\"")) {
            String dec = ContextoRiscV.decodificarEscapes(v.substring(1, v.length() - 1));
            String et = ctx.registrarCadena(dec);
            ctx.emitir("    la " + vreg + ", " + et);
            guardarEnBase(base, vreg, false, off);
            return;
        }
        Matcher m = P_LOCAL.matcher(v);
        if (m.matches()) {
            if (tc == TipoRV.DOBLE) {
                AccesoMemoria.emitir(ctx, "fld", "ft0", "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
                guardarEnBase(base, "ft0", true, off);
            } else {
                AccesoMemoria.emitir(ctx, "lw", vreg, "s0", ctx.slotByte(Integer.parseInt(m.group(1))));
                guardarEnBase(base, vreg, false, off);
            }
            return;
        }
        m = P_GLOBAL.matcher(v);
        if (m.matches()) {
            String gb = "t2".equals(vreg) ? "t3" : "t2";
            ctx.emitir("    la " + gb + ", Globals");
            if (tc == TipoRV.DOBLE) {
                AccesoMemoria.emitir(ctx, "fld", "ft0", gb, ctx.globalByte(Integer.parseInt(m.group(1))));
                guardarEnBase(base, "ft0", true, off);
            } else {
                AccesoMemoria.emitir(ctx, "lw", vreg, gb, ctx.globalByte(Integer.parseInt(m.group(1))));
                guardarEnBase(base, vreg, false, off);
            }
            return;
        }
        if (v.startsWith("{") || v.startsWith("&(")
                || (v.startsWith("(") && v.contains("[]){"))) {
            String innerTipo = null;
            TipoRV innerElem = TipoRV.ENTERO;
            if (v.startsWith("&(struct ")) {
                innerTipo = v.substring(9, v.indexOf(')')).trim();
            }
            int abre = v.indexOf('{');
            int cierra = v.lastIndexOf('}');
            List<String> inner = partirPorComas(v.substring(abre + 1, cierra));
            int n = Math.max(1, inner.size());
            ctx.emitir("    mv " + vreg + ", s1");
            int bytes = n * 8;
            if (bytes <= 2047) {
                ctx.emitir("    addi s1, s1, " + bytes);
            } else {
                ctx.emitir("    li t3, " + bytes);
                ctx.emitir("    add s1, s1, t3");
            }
            llenarLiteral(innerTipo, innerElem, inner, depth + 1);
            guardarEnBase(base, vreg, false, off);
            return;
        }
        ctx.error("valor de literal no reconocido: " + v);
    }

    private void guardarEnBase(String base, String src, boolean esFloat, int off) {
        String instr = esFloat ? "fsd" : "sw";
        if (off <= 2047) {
            ctx.emitir("    " + instr + " " + src + ", " + off + "(" + base + ")");
        } else {
            ctx.emitir("    li t6, " + off);
            ctx.emitir("    add t6, " + base + ", t6");
            ctx.emitir("    " + instr + " " + src + ", 0(t6)");
        }
    }

    private static List<String> partirPorComas(String s) {
        List<String> partes = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int prof = 0;
        boolean enComillas = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (enComillas) {
                cur.append(c);
                if (c == '\\' && i + 1 < s.length()) {
                    cur.append(s.charAt(++i));
                } else if (c == '"') {
                    enComillas = false;
                }
                continue;
            }
            if (c == '"') {
                enComillas = true;
                cur.append(c);
                continue;
            }
            if (c == '{' || c == '(' || c == '[') prof++;
            if (c == '}' || c == ')' || c == ']') prof--;
            if (c == ',' && prof == 0) {
                partes.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        partes.add(cur.toString());
        return partes;
    }

    // ------------------------------------------------------------------ arreglos

    public void declararArreglo(DeclararArreglo3D arreglo) {
        long total = totalDimensiones(arreglo.getDims());
        int bytes = (int) (Math.max(1, total) * 8);
        avanzarHeap(bytes);
        NameAccess na = ctx.accesoDeNombre(arreglo.getNombre());
        if (na == null) return;
        if (na.isGlobal()) {
            if (na.getOffset() < 0) return;
            ctx.emitir("    la t1, Globals");
            AccesoMemoria.emitir(ctx, "sw", "t0", "t1", ctx.globalByte(na.getOffset()));
        } else if (na.getOffset() >= 0) {
            AccesoMemoria.emitir(ctx, "sw", "t0", "s0", ctx.slotByte(na.getOffset()));
        }
    }

    private long totalDimensiones(String dims) {
        if (dims == null || dims.isBlank()) return 1;
        long total = 1;
        for (String parte : dims.split(",")) {
            try {
                total *= Math.max(1, Long.parseLong(parte.trim()));
            } catch (NumberFormatException e) {
                return 64;
            }
        }
        return total;
    }

    // -------------------------------------------------------------------- cargas

    public void cargarEntero(MemoryAccess op, String reg) {
        cargar(op, reg, false, 0);
    }

    public void cargarEntero(MemoryAccess op, String reg, int depth) {
        cargar(op, reg, false, depth);
    }

    public void cargarDouble(MemoryAccess op, String freg) {
        cargar(op, freg, true, 0);
    }

    private void cargar(MemoryAccess op, String reg, boolean doble, int depth) {
        if (op == null) {
            cero(reg, doble);
            return;
        }
        if (op instanceof Literal3D lit) {
            if (doble) {
                literalDouble(lit, reg);
            } else {
                literalEntero(lit, reg);
            }
            return;
        }
        if (op instanceof NameAccess na) {
            cargarNombre(na, reg, doble);
            return;
        }
        if (op instanceof TemporalAccess ta) {
            cargarTemporal(ta, reg, doble);
            return;
        }
        if (op instanceof CampoAccess || op instanceof IndiceAccess) {
            direccionPool(op, depth);
            emitirCarga(POOL[depth], 0, reg, doble);
            return;
        }
        if (op instanceof LabelAccess) {
            ctx.error("etiqueta usada como valor");
            cero(reg, doble);
            return;
        }
        ctx.error("operando no soportado: " + op.getClass().getSimpleName());
        cero(reg, doble);
    }

    private void cargarNombre(NameAccess na, String reg, boolean doble) {
        if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
            String addr = doble ? "t0" : reg;
            emitirCarga("s0", ctx.slotByte(2), addr, false);
            if (na.isAutoreferencia()) {
                sumarOffset(addr, Math.max(0, na.getOffset()) * 8);
            }
            if (doble || na.isAutoreferencia()) {
                emitirCarga(addr, 0, reg, doble);
            }
            return;
        }
        if (na.getOffset() < 0) {
            String mensaje = !doble && na.isGlobal() ? "global sin slot: " : "variable sin slot: ";
            ctx.error(mensaje + na.getNombre());
            cero(reg, doble);
            return;
        }
        if (na.isGlobal()) {
            String base = doble ? "t0" : reg;
            ctx.emitir("    la " + base + ", Globals");
            emitirCarga(base, ctx.globalByte(na.getOffset()), reg, doble);
            return;
        }
        emitirCarga("s0", ctx.slotByte(na.getOffset()), reg, doble);
    }

    private void cargarTemporal(TemporalAccess ta, String reg, boolean doble) {
        if (ta.getOffset() < 0) {
            ctx.error("temporal sin slot: t" + ta.getNumero());
            cero(reg, doble);
            return;
        }
        int offset = ctx.slotByte(ta.getOffset());
        if (doble) {
            emitirCarga("s0", offset, reg, true);
        } else if (ctx.getTipoRV(ta) == TipoRV.DOBLE) {
            emitirCarga("s0", offset, "ft0", true);
            ctx.emitir("    fcvt.w.d " + reg + ", ft0, rtz");
        } else {
            emitirCarga("s0", offset, reg, false);
        }
    }

    private void literalEntero(Literal3D lit, String reg) {
        String txt = lit.getTexto() == null ? "0" : lit.getTexto().trim();
        if (txt.startsWith("\"")) {
            String dec = ContextoRiscV.decodificarEscapes(txt.substring(1, txt.length() - 1));
            String et = ctx.registrarCadena(dec);
            ctx.emitir("    la " + reg + ", " + et);
            return;
        }
        switch (txt) {
            case "NULL" -> ctx.emitir("    li " + reg + ", 0");
            case "H" -> ctx.emitir("    mv " + reg + ", s1");
            case "S" -> ctx.emitir("    mv " + reg + ", s2");
            case "self" -> AccesoMemoria.emitir(ctx, "lw", reg, "s0", ctx.slotByte(2));
            default -> {
                if (txt.matches("^'[^']+'$")) {
                    ctx.emitir("    li " + reg + ", " + (int) txt.charAt(1));
                } else if (txt.matches("^[+-]?\\d+$")) {
                    ctx.emitir("    li " + reg + ", " + txt);
                } else if (txt.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
                    String et = ctx.registrarConstante(txt);
                    String tmp = esPool(reg) ? reg : "t0";
                    ctx.emitir("    la " + tmp + ", " + et);
                    ctx.emitir("    fld ft0, 0(" + tmp + ")");
                    ctx.emitir("    fcvt.w.d " + reg + ", ft0, rtz");
                } else {
                    ctx.error("literal no reconocido: " + txt);
                    ctx.emitir("    li " + reg + ", 0");
                }
            }
        }
    }

    private void literalDouble(Literal3D lit, String freg) {
        String txt = lit.getTexto() == null ? "0" : lit.getTexto().trim();
        if (txt.matches("^[+-]?\\d+$")) {
            ctx.emitir("    li t0, " + txt);
            ctx.emitir("    fcvt.d.w " + freg + ", t0");
            return;
        }
        if (txt.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            String et = ctx.registrarConstante(txt);
            ctx.emitir("    la t0, " + et);
            ctx.emitir("    fld " + freg + ", 0(t0)");
            return;
        }
        if (txt.startsWith("\"")) {
            String dec = ContextoRiscV.decodificarEscapes(txt.substring(1, txt.length() - 1));
            String et = ctx.registrarCadena(dec);
            ctx.emitir("    la t0, " + et);
            ctx.emitir("    fcvt.d.w " + freg + ", t0");
            return;
        }
        if ("NULL".equals(txt) || "H".equals(txt) || "S".equals(txt) || "self".equals(txt)) {
            literalEntero(lit, "t0");
            ctx.emitir("    fcvt.d.w " + freg + ", t0");
            return;
        }
        ctx.error("literal double no reconocido: " + txt);
        ctx.emitir("    fcvt.d.w " + freg + ", zero");
    }

    // direccion de un acceso por heap (campo o indice) en el registro del pool

    private void direccionPool(MemoryAccess op, int depth) {
        if (depth >= POOL.length || depth + 1 >= POOL.length) {
            throw new IllegalStateException("acceso demasiado anidado");
        }
        String r = POOL[depth];
        if (op instanceof CampoAccess ca) {
            cargarEntero(ca.getBase(), POOL[depth], depth);
            sumarOffset(r, Math.max(0, ca.getOffset()) * 8);
        } else if (op instanceof IndiceAccess ia) {
            cargarEntero(ia.getBase(), POOL[depth], depth);
            String ir = POOL[depth + 1];
            cargarEntero(ia.getIndice(), POOL[depth + 1], depth + 1);
            int limite = ctx.limiteArreglo(ia.getBase());
            if (limite > 0) {
                ctx.emitir("    bltz " + ir + ", rt_error_indice");
                ctx.emitir("    li t6, " + limite);
                ctx.emitir("    bge " + ir + ", t6, rt_error_indice");
            }
            ctx.emitir("    slli " + ir + ", " + ir + ", 3");
            ctx.emitir("    add " + r + ", " + r + ", " + ir);
        } else {
            throw new IllegalStateException("direccion de operando simple");
        }
    }

    // ------------------------------------------------------------------ guardado

    public void guardarEntero(MemoryAccess dest, String reg) {
        if (dest == null) return;
        if (ctx.getTipoRV(dest) == TipoRV.DOBLE) {
            ctx.emitir("    fcvt.d.w ft0, " + reg);
            guardarDouble(dest, "ft0");
            return;
        }
        guardarPalabra(dest, reg, "sw");
    }

    public void guardarDouble(MemoryAccess dest, String freg) {
        if (dest == null) return;
        if (ctx.getTipoRV(dest) != TipoRV.DOBLE) {
            ctx.emitir("    fcvt.w.d t0, " + freg + ", rtz");
            guardarPalabra(dest, "t0", "sw");
            return;
        }
        guardarPalabra(dest, freg, "fsd");
    }

    private void guardarPalabra(MemoryAccess dest, String src, String instr) {
        if (dest instanceof NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                int depth = esPool(src) ? indicePool(src) + 1 : 0;
                String addr = POOL[depth];
                AccesoMemoria.emitir(ctx, "lw", addr, "s0", ctx.slotByte(2));
                if (na.isAutoreferencia()) {
                    sumarOffset(addr, Math.max(0, na.getOffset()) * 8);
                }
                ctx.emitir("    " + instr + " " + src + ", 0(" + addr + ")");
                return;
            }
            if (na.isGlobal()) {
                if (na.getOffset() < 0) {
                    ctx.error("global sin slot: " + na.getNombre());
                    return;
                }
                String base = "t1".equals(src) ? "t2" : "t1";
                ctx.emitir("    la " + base + ", Globals");
                AccesoMemoria.emitir(ctx, instr, src, base, ctx.globalByte(na.getOffset()));
                return;
            }
            if (na.getOffset() < 0) {
                ctx.error("variable sin slot: " + na.getNombre());
                return;
            }
            AccesoMemoria.emitir(ctx, instr, src, "s0", ctx.slotByte(na.getOffset()));
            return;
        }
        if (dest instanceof TemporalAccess ta) {
            if (ta.getOffset() < 0) {
                ctx.error("temporal sin slot: t" + ta.getNumero());
                return;
            }
            AccesoMemoria.emitir(ctx, instr, src, "s0", ctx.slotByte(ta.getOffset()));
            return;
        }
        if (dest instanceof CampoAccess || dest instanceof IndiceAccess) {
            int depth = esPool(src) ? indicePool(src) + 1 : 0;
            direccionPool(dest, depth);
            ctx.emitir("    " + instr + " " + src + ", 0(" + POOL[depth] + ")");
            return;
        }
        ctx.error("destino no soportado: " + dest.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------- varios

    private void emitirCarga(String base, int offset, String reg, boolean doble) {
        AccesoMemoria.emitir(ctx, doble ? "fld" : "lw", reg, base, offset);
    }

    private void cero(String reg, boolean doble) {
        ctx.emitir(doble
                ? "    fcvt.d.w " + reg + ", zero"
                : "    li " + reg + ", 0");
    }

    private void sumarOffset(String reg, int bytes) {
        if (bytes == 0) return;
        if (bytes >= -2048 && bytes <= 2047) {
            ctx.emitir("    addi " + reg + ", " + reg + ", " + bytes);
        } else {
            ctx.emitir("    li t6, " + bytes);
            ctx.emitir("    add " + reg + ", " + reg + ", t6");
        }
    }

    private static boolean esPool(String reg) {
        for (String p : POOL) {
            if (p.equals(reg)) return true;
        }
        return false;
    }

    private static int indicePool(String reg) {
        for (int i = 0; i < POOL.length; i++) {
            if (POOL[i].equals(reg)) return i;
        }
        return -1;
    }
}
