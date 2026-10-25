package cunoc.compi2.alien_code.codegen.riscv;

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
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

public final class RiscVMemoria {
    public enum TipoLiteral {
        ENTERO, DECIMAL, CADENA, NULO, SELF, CLASE, ESTRUCTURA, MALLOC, OTRO
    }

    public record ValorLiteral(TipoLiteral tipo, String texto) {
    }

    private final ContextoRiscV ctx;
    private final RiscVTexto texto;
    private final RiscVDatos datos;

    public RiscVMemoria(ContextoRiscV ctx, RiscVTexto texto, RiscVDatos datos) {
        this.ctx = ctx;
        this.texto = texto;
        this.datos = datos;
    }

    public void cargar(MemoryAccess op) {
        cargarEn(op, "ft0");
    }

    public void cargarEn(MemoryAccess op, String fdest) {
        if (op == null) {
            cero(fdest);
            return;
        }
        if (op instanceof Literal3D lit) {
            cargarLiteral(lit, fdest);
            return;
        }
        if (op instanceof NameAccess na) {
            cargarNombre(na, fdest);
            return;
        }
        if (op instanceof TemporalAccess ta) {
            cargarTemporal(ta, fdest);
            return;
        }
        if (op instanceof CampoAccess campo) {
            cargarEn(campo.getBase(), "ft1");
            ctx.emitir("    fcvt.w.d t0, ft1, rtz");
            if (campo.getOffset() != 0) {
                ctx.emitir("    addi t0, t0, " + campo.getOffset());
            }
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        if (op instanceof IndiceAccess indice) {
            cargarEn(indice.getBase(), "ft10");
            cargarEn(indice.getIndice(), "ft11");
            ctx.emitir("    fadd.d ft10, ft10, ft11");
            ctx.emitir("    fcvt.w.d t0, ft10, rtz");
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        if (op instanceof LabelAccess etiqueta) {
            ctx.error("no se puede cargar una etiqueta: " + etiqueta.getNombre());
            cero(fdest);
            return;
        }
        texto.evaluarEn(expresionC(op), fdest);
    }

    public void guardar(MemoryAccess destino) {
        ctx.emitir("    fmv.d fs0, ft0");
        direccion(destino);
        ctx.emitir("    fsd fs0, 0(t0)");
    }

    public void guardarInmediato(MemoryAccess destino, long valor) {
        ctx.emitir("    li t0, " + valor);
        ctx.emitir("    fcvt.d.w ft0, t0");
        guardar(destino);
    }

    public void asignar(Asignacion3D asg) {
        MemoryAccess destino = asg.getResult();
        MemoryAccess valor = asg.getOperand1();
        if (valor == null) {
            guardarInmediato(destino, 0);
            return;
        }
        if (valor instanceof Literal3D lit) {
            ValorLiteral v = clasificar(lit);
            switch (v.tipo()) {
                case MALLOC -> asignarMalloc(destino, lit, v.texto());
                case ESTRUCTURA -> asignarEstructura(destino, v.texto());
                case CLASE -> {
                    ctx.error("literal de clase inesperado: " + v.texto());
                    guardarInmediato(destino, 0);
                }
                default -> {
                    cargarLiteral(lit, "ft0");
                    guardar(destino);
                }
            }
            return;
        }
        cargarEn(valor, "ft0");
        guardar(destino);
    }

    public void declararArreglo(DeclararArreglo3D arreglo) {
        Symbol simbolo = ctx.simbolo(arreglo.getNombre());
        int offset = simbolo == null ? -1 : simbolo.getOffset();
        if (offset < 0) {
            ctx.error("arreglo sin offset: " + arreglo.getNombre());
            ctx.emitir("    # FIXME arreglo sin offset: " + arreglo.getNombre());
            return;
        }
        long total = tamanoArreglo(arreglo.getDims());
        if (simbolo.isOffsetGlobal()) {
            direccionGlobal(offset);
        } else {
            direccionLocal(offset);
        }
        ctx.emitir("    fcvt.d.w ft0, s4");
        ctx.emitir("    fsd ft0, 0(t0)");
        sumarHeap(Math.max(1, total));
    }

    public void leer(Leer3D leer) {
        String formato = leer.getFormato() == null ? "" : leer.getFormato().trim();
        cargar(leer.getResult());
        switch (formato) {
            case "%s" -> {
                ctx.emitir("    fcvt.w.d a0, ft0, rtz");
                ctx.emitir("    jal ra, rt_read_str");
            }
            case "%lf" -> {
                ctx.emitir("    jal ra, rt_read_double");
                ctx.emitir("    fmv.d ft0, fa0");
                guardar(leer.getResult());
            }
            case "%c" -> {
                ctx.emitir("    jal ra, rt_read_char");
                ctx.emitir("    fcvt.d.w ft0, a0");
                guardar(leer.getResult());
            }
            default -> {
                ctx.emitir("    jal ra, rt_read_int");
                ctx.emitir("    fcvt.d.w ft0, a0");
                guardar(leer.getResult());
            }
        }
    }

    public Type tipoDe(MemoryAccess op) {
        if (op instanceof TemporalAccess ta) {
            return tipoDesdeCType(ctx.tiposTemporales().get(ta.getNumero()));
        }
        return op == null ? null : op.getTipo();
    }

    public boolean esBooleano(MemoryAccess op) {
        return tipoDe(op) == Type.BOOL;
    }

    public void guardarSlot(int offset) {
        direccionLocal(offset);
        ctx.emitir("    fsd ft0, 0(t0)");
    }

    public void guardarInmediatoSlot(int offset, long valor) {
        ctx.emitir("    li t0, " + valor);
        ctx.emitir("    fcvt.d.w ft0, t0");
        guardarSlot(offset);
    }

    public ValorLiteral clasificar(Literal3D lit) {
        String t = lit.getTexto() == null ? "" : lit.getTexto().trim();
        if (lit.getTipo() == Type.STRING && t.startsWith("\"")) {
            return new ValorLiteral(TipoLiteral.CADENA, t);
        }
        if ("NULL".equals(t)) {
            return new ValorLiteral(TipoLiteral.NULO, t);
        }
        if ("self".equals(t)) {
            return new ValorLiteral(TipoLiteral.SELF, t);
        }
        if ("H".equals(t) || "S".equals(t)) {
            return new ValorLiteral(TipoLiteral.ENTERO, t);
        }
        if (t.startsWith("&(")) {
            int abre = t.indexOf('{');
            int cierra = t.lastIndexOf('}');
            if (abre >= 0 && cierra > abre) {
                return new ValorLiteral(TipoLiteral.ESTRUCTURA, t.substring(abre, cierra + 1));
            }
            return new ValorLiteral(TipoLiteral.OTRO, t);
        }
        if (t.startsWith("{")) {
            return new ValorLiteral(TipoLiteral.ESTRUCTURA, t);
        }
        if (t.startsWith("malloc(")) {
            return new ValorLiteral(TipoLiteral.MALLOC, t);
        }
        if (t.matches("^[+-]?\\d+$")) {
            return new ValorLiteral(TipoLiteral.ENTERO, t);
        }
        if (t.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            return new ValorLiteral(TipoLiteral.DECIMAL, t);
        }
        return new ValorLiteral(TipoLiteral.OTRO, t);
    }

    private void cargarLiteral(Literal3D lit, String fdest) {
        ValorLiteral v = clasificar(lit);
        switch (v.tipo()) {
            case CADENA -> {
                int base = datosBase(v.texto());
                ctx.emitir("    li t0, " + base);
                ctx.emitir("    fcvt.d.w " + fdest + ", t0");
            }
            case NULO -> cero(fdest);
            case SELF -> {
                cargarNombre(new NameAccess("self", Type.CLASS, "self", true, 0), fdest);
            }
            case ESTRUCTURA, MALLOC -> {
                ctx.error("literal no cargable: " + v.texto());
                cero(fdest);
            }
            default -> texto.evaluarEn(v.texto(), fdest);
        }
    }

    private void cargarNombre(NameAccess na, String fdest) {
        if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
            cargarStackAbsoluto(2, "ft1");
            ctx.emitir("    fcvt.w.d t0, ft1, rtz");
            if (na.getOffset() != 0) {
                ctx.emitir("    addi t0, t0, " + na.getOffset());
            }
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        if (na.getOffset() < 0) {
            ctx.error("simbolo sin offset: " + na.getNombre());
            cero(fdest);
            return;
        }
        if (na.isGlobal()) {
            ctx.emitir("    li t0, " + (na.getOffset() * 8L));
            ctx.emitir("    add t0, s0, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        direccionLocal(na.getOffset());
        ctx.emitir("    fld " + fdest + ", 0(t0)");
    }

    private void cargarTemporal(TemporalAccess ta, String fdest) {
        if (ta.getOffset() < 0) {
            ctx.error("temporal sin offset: t" + ta.getNumero());
            cero(fdest);
            return;
        }
        direccionLocal(ta.getOffset());
        ctx.emitir("    fld " + fdest + ", 0(t0)");
    }

    private void direccion(MemoryAccess op) {
        if (op == null) {
            ctx.error("direccion de operando nulo");
            ctx.emitir("    mv t0, zero");
            return;
        }
        if (op instanceof NameAccess na) {
            if (na.isAutoreferencia() || "self".equals(na.getNombre())) {
                cargarStackAbsoluto(2, "ft1");
                ctx.emitir("    fcvt.w.d t0, ft1, rtz");
                if (na.getOffset() != 0) {
                    ctx.emitir("    addi t0, t0, " + na.getOffset());
                }
                ctx.emitir("    slli t0, t0, 3");
                ctx.emitir("    add t0, s1, t0");
                return;
            }
            if (na.isGlobal()) {
                direccionGlobal(na.getOffset());
            } else {
                direccionLocal(na.getOffset());
            }
            return;
        }
        if (op instanceof TemporalAccess ta) {
            direccionLocal(ta.getOffset());
            return;
        }
        if (op instanceof CampoAccess campo) {
            cargarEn(campo.getBase(), "ft1");
            ctx.emitir("    fcvt.w.d t0, ft1, rtz");
            if (campo.getOffset() != 0) {
                ctx.emitir("    addi t0, t0, " + campo.getOffset());
            }
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            return;
        }
        if (op instanceof IndiceAccess indice) {
            cargarEn(indice.getBase(), "ft10");
            cargarEn(indice.getIndice(), "ft11");
            ctx.emitir("    fadd.d ft10, ft10, ft11");
            ctx.emitir("    fcvt.w.d t0, ft10, rtz");
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            return;
        }
        ctx.error("direccion no soportada: " + op.getClass().getSimpleName());
        ctx.emitir("    mv t0, zero");
    }

    private void direccionLocal(int offset) {
        if (offset == 0) {
            ctx.emitir("    mv t0, s3");
        } else {
            ctx.emitir("    addi t0, s3, " + offset);
        }
        ctx.emitir("    slli t0, t0, 3");
        ctx.emitir("    add t0, s0, t0");
    }

    private void direccionGlobal(int offset) {
        ctx.emitir("    li t0, " + (offset * 8L));
        ctx.emitir("    add t0, s0, t0");
    }

    private void cargarStackAbsoluto(int offset, String fdest) {
        if (offset == 0) {
            ctx.emitir("    mv t0, s3");
        } else {
            ctx.emitir("    addi t0, s3, " + offset);
        }
        ctx.emitir("    slli t0, t0, 3");
        ctx.emitir("    add t0, s0, t0");
        ctx.emitir("    fld " + fdest + ", 0(t0)");
    }

    private void sumarHeap(long cantidad) {
        if (cantidad == 1) {
            ctx.emitir("    addi s4, s4, 1");
        } else if (cantidad > 1) {
            ctx.emitir("    li t0, " + cantidad);
            ctx.emitir("    add s4, s4, t0");
        }
    }

    private void asignarMalloc(MemoryAccess destino, Literal3D lit, String textoLiteral) {
        String interno = extraerInternoMalloc(textoLiteral);
        if ("malloc(256)".equals(textoLiteral) && lit.getTipo() == Type.STRING) {
            ctx.emitir("    fcvt.d.w ft0, s5");
            guardar(destino);
            ctx.emitir("    addi s5, s5, 256");
            return;
        }
        String tipoClase = interno.startsWith("sizeof(")
                ? interno.substring("sizeof(".length(), interno.length() - 1).trim()
                : interno;
        ClaseLayouts layouts = ctx.layouts();
        if (layouts != null && layouts.contiene(tipoClase)) {
            long tamano = Math.max(1, layouts.tamanoDe(tipoClase));
            ctx.emitir("    mv t2, s4");
            ctx.emitir("    fcvt.d.w ft0, s4");
            guardar(destino);
            sumarHeap(tamano);
            ctx.emitir("    mv t0, t2");
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    li t1, " + layouts.idDe(tipoClase));
            ctx.emitir("    fcvt.d.w ft0, t1");
            ctx.emitir("    fsd ft0, 0(t0)");
            return;
        }
        String cuenta = interno;
        int producto = interno.indexOf(" * ");
        if (producto >= 0) {
            cuenta = interno.substring(0, producto).trim();
        }
        ctx.emitir("    mv t2, s4");
        texto.cargarEntero(cuenta, "t3");
        ctx.emitir("    fcvt.d.w ft0, s4");
        guardar(destino);
        ctx.emitir("    add s4, s4, t3");
    }

    private void asignarEstructura(MemoryAccess destino, String textoLiteral) {
        int abre = textoLiteral.indexOf('{');
        int cierra = textoLiteral.lastIndexOf('}');
        if (abre < 0 || cierra <= abre) {
            guardarInmediato(destino, 0);
            return;
        }
        List<String> valores = dividirNivelSuperior(textoLiteral.substring(abre + 1, cierra));
        int cantidad = Math.max(1, valores.size());
        ctx.emitir("    mv t5, s4");
        ctx.emitir("    fcvt.d.w ft0, s4");
        guardar(destino);
        sumarHeap(cantidad);
        llenarHeap(valores);
    }

    private void llenarHeap(List<String> valores) {
        for (int i = 0; i < valores.size(); i++) {
            String valor = valores.get(i).trim();
            if (valor.isEmpty()) {
                valor = "0";
            }
            if (valor.startsWith("{") && valor.endsWith("}")) {
                List<String> hijos = dividirNivelSuperior(valor.substring(1, valor.length() - 1));
                int cantidad = Math.max(1, hijos.size());
                ctx.emitir("    addi sp, sp, -16");
                ctx.emitir("    sw t5, 0(sp)");
                ctx.emitir("    mv t6, t5");
                ctx.emitir("    mv t5, s4");
                sumarHeap(cantidad);
                ctx.emitir("    addi t0, t6, " + i);
                ctx.emitir("    slli t0, t0, 3");
                ctx.emitir("    add t0, s1, t0");
                ctx.emitir("    fcvt.d.w ft0, t5");
                ctx.emitir("    fsd ft0, 0(t0)");
                llenarHeap(hijos);
                ctx.emitir("    lw t5, 0(sp)");
                ctx.emitir("    addi sp, sp, 16");
                continue;
            }
            texto.evaluar(valor);
            ctx.emitir("    addi t0, t5, " + i);
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fsd ft0, 0(t0)");
        }
    }

    private String extraerInternoMalloc(String textoLiteral) {
        int abre = textoLiteral.indexOf('(');
        int cierra = textoLiteral.lastIndexOf(')');
        if (abre < 0 || cierra <= abre) {
            return "";
        }
        return textoLiteral.substring(abre + 1, cierra).trim();
    }

    private List<String> dividirNivelSuperior(String textoLiteral) {
        List<String> partes = new ArrayList<>();
        int nivel = 0;
        int inicio = 0;
        for (int i = 0; i < textoLiteral.length(); i++) {
            char ch = textoLiteral.charAt(i);
            if (ch == '{' || ch == '(' || ch == '[') {
                nivel++;
            } else if (ch == '}' || ch == ')' || ch == ']') {
                nivel--;
            } else if (ch == ',' && nivel == 0) {
                partes.add(textoLiteral.substring(inicio, i));
                inicio = i + 1;
            }
        }
        String ultimo = textoLiteral.substring(inicio);
        if (!ultimo.trim().isEmpty() || !partes.isEmpty()) {
            partes.add(ultimo);
        }
        return partes;
    }

    private long tamanoArreglo(String dims) {
        if (dims == null || dims.isBlank()) {
            return 1;
        }
        long total = 1;
        for (String parte : dims.split(",")) {
            String limpio = parte.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            try {
                long valor = Long.parseLong(limpio);
                total *= Math.max(1, valor);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return total;
    }

    private int datosBase(String literal) {
        return datos.baseDeCadena(literal);
    }

    private String expresionC(MemoryAccess op) {
        StringBuilder sb = new StringBuilder();
        op.toCCode(sb);
        return sb.toString();
    }

    private Type tipoDesdeCType(String ctype) {
        if (ctype == null) {
            return Type.INT;
        }
        if (ctype.contains("bool")) {
            return Type.BOOL;
        }
        if (ctype.contains("double") || ctype.contains("float")) {
            return Type.FLOAT;
        }
        if (ctype.contains("struct") || ctype.endsWith("*")) {
            return Type.CLASS;
        }
        if (ctype.contains("char")) {
            return Type.STRING;
        }
        return Type.INT;
    }

    private void cero(String fdest) {
        ctx.emitir("    fcvt.d.w " + fdest + ", zero");
    }
}
