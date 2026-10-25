package cunoc.compi2.alien_code.codegen.riscv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RiscVTexto {
    private static final Pattern STACK_LOCAL = Pattern.compile(
            "^Stack\\[\\s*\\(int\\)\\s*\\(\\s*P\\s*([+-])\\s*(\\d+)\\s*\\)\\s*\\]$");
    private static final Pattern STACK_GLOBAL = Pattern.compile(
            "^Stack\\[\\s*\\(int\\)\\s*(\\d+)\\s*\\]$");
    private static final Pattern ENTERO = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern DECIMAL = Pattern.compile(
            "^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$");
    private static final String PREFIJO_HEAP = "Heap[(int)(";

    private final ContextoRiscV ctx;
    private final RiscVDatos datos;

    public RiscVTexto(ContextoRiscV ctx, RiscVDatos datos) {
        this.ctx = ctx;
        this.datos = datos;
    }

    public void evaluar(String texto) {
        evaluarEn(texto, "ft0");
    }

    public void evaluarEn(String texto, String fdest) {
        String t = texto == null ? "0" : texto.trim();
        if (t.isEmpty()) {
            cero(fdest);
            return;
        }
        int posicion = posicionComparacion(t);
        if (posicion >= 0) {
            evaluarComparacion(t, posicion);
            if (!"ft0".equals(fdest)) {
                ctx.emitir("    fmv.d " + fdest + ", ft0");
            }
            return;
        }
        if (t.startsWith("Stack[")) {
            cargarStack(t, fdest);
            return;
        }
        if (t.startsWith("Heap[")) {
            cargarHeap(t, fdest);
            return;
        }
        if ("NULL".equals(t)) {
            cero(fdest);
            return;
        }
        if ("self".equals(t)) {
            cargarStack("Stack[(int)(P + 2)]", fdest);
            return;
        }
        if ("H".equals(t)) {
            ctx.emitir("    fcvt.d.w " + fdest + ", s4");
            return;
        }
        if ("S".equals(t)) {
            ctx.emitir("    fcvt.d.w " + fdest + ", s5");
            return;
        }
        if (t.startsWith("\"")) {
            int base = datos.baseDeCadena(t);
            ctx.emitir("    li t0, " + base);
            ctx.emitir("    fcvt.d.w " + fdest + ", t0");
            return;
        }
        if (ENTERO.matcher(t).matches()) {
            cargarEnteroLiteral(t, "t0");
            ctx.emitir("    fcvt.d.w " + fdest + ", t0");
            return;
        }
        if (DECIMAL.matcher(t).matches()) {
            String etiqueta = datos.etiquetaConstante(t);
            ctx.emitir("    fld " + fdest + ", " + etiqueta);
            return;
        }
        ctx.error("texto C no reconocido: " + t);
        ctx.emitir("    # FIXME texto: " + t);
        cero(fdest);
    }

    public void cargarEntero(String texto, String ireg) {
        String t = texto == null ? "0" : texto.trim();
        if (ENTERO.matcher(t).matches()) {
            cargarEnteroLiteral(t, ireg);
            return;
        }
        if (t.isEmpty()) {
            ctx.emitir("    li " + ireg + ", 0");
            return;
        }
        evaluar(t);
        ctx.emitir("    fcvt.w.d " + ireg + ", ft0, rtz");
    }

    private void cargarEnteroLiteral(String texto, String ireg) {
        long valor;
        try {
            valor = Long.parseLong(texto);
        } catch (NumberFormatException e) {
            valor = 0;
            ctx.error("entero fuera de rango: " + texto);
        }
        ctx.emitir("    li " + ireg + ", " + valor);
    }

    private void cargarStack(String texto, String fdest) {
        Matcher local = STACK_LOCAL.matcher(texto);
        if (local.matches()) {
            int offset = Integer.parseInt(local.group(2));
            if ("-".equals(local.group(1))) {
                offset = -offset;
            }
            dirStack(offset, "t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        Matcher global = STACK_GLOBAL.matcher(texto);
        if (global.matches()) {
            int offset = Integer.parseInt(global.group(1));
            ctx.emitir("    li t0, " + (offset * 8L));
            ctx.emitir("    add t0, s0, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        ctx.error("Stack no reconocido: " + texto);
        ctx.emitir("    # FIXME stack: " + texto);
        cero(fdest);
    }

    private void cargarHeap(String texto, String fdest) {
        int inicio = texto.indexOf(PREFIJO_HEAP);
        if (inicio != 0) {
            ctx.error("Heap no reconocido: " + texto);
            cero(fdest);
            return;
        }
        int abre = PREFIJO_HEAP.length() - 1;
        int cierra = parentesisQueCierra(texto, abre);
        if (cierra < 0) {
            ctx.error("Heap sin parentesis de cierre: " + texto);
            cero(fdest);
            return;
        }
        String interno = texto.substring(PREFIJO_HEAP.length(), cierra).trim();
        String resto = texto.substring(cierra + 1).trim();
        if (resto.startsWith("+") && resto.endsWith("]")) {
            String numero = resto.substring(1, resto.length() - 1).trim();
            int offset;
            try {
                offset = Integer.parseInt(numero);
            } catch (NumberFormatException e) {
                ctx.error("offset de campo invalido: " + resto);
                offset = 0;
            }
            evaluarEn(interno, "ft1");
            ctx.emitir("    fcvt.w.d t0, ft1, rtz");
            if (offset != 0) {
                ctx.emitir("    addi t0, t0, " + offset);
            }
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        if ("]".equals(resto)) {
            List<String> partes = dividirSuma(interno);
            if (partes.isEmpty()) {
                ctx.error("indice vacio: " + texto);
                cero(fdest);
                return;
            }
            evaluarEn(partes.get(0), "ft10");
            for (int i = 1; i < partes.size(); i++) {
                evaluarEn(partes.get(i), "ft11");
                ctx.emitir("    fadd.d ft10, ft10, ft11");
            }
            ctx.emitir("    fcvt.w.d t0, ft10, rtz");
            ctx.emitir("    slli t0, t0, 3");
            ctx.emitir("    add t0, s1, t0");
            ctx.emitir("    fld " + fdest + ", 0(t0)");
            return;
        }
        ctx.error("Heap no reconocido: " + texto);
        ctx.emitir("    # FIXME heap: " + texto);
        cero(fdest);
    }

    private void evaluarComparacion(String texto, int posicion) {
        String operador = operadorEn(texto, posicion);
        String izquierda = texto.substring(0, posicion).trim();
        String derecha = texto.substring(posicion + operador.length()).trim();
        evaluarEn(izquierda, "fs0");
        evaluarEn(derecha, "ft0");
        switch (operador) {
            case "<" -> ctx.emitir("    flt.d a0, fs0, ft0");
            case ">" -> ctx.emitir("    flt.d a0, ft0, fs0");
            case "<=" -> ctx.emitir("    fle.d a0, fs0, ft0");
            case ">=" -> ctx.emitir("    fle.d a0, ft0, fs0");
            case "==" -> ctx.emitir("    feq.d a0, fs0, ft0");
            case "!=" -> {
                ctx.emitir("    feq.d a0, fs0, ft0");
                ctx.emitir("    xori a0, a0, 1");
            }
            default -> ctx.error("comparacion no reconocida: " + operador);
        }
        ctx.emitir("    fcvt.d.w ft0, a0");
    }

    private int posicionComparacion(String texto) {
        int parentesis = 0;
        int corchetes = 0;
        int llaves = 0;
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            switch (ch) {
                case '(' -> parentesis++;
                case ')' -> parentesis--;
                case '[' -> corchetes++;
                case ']' -> corchetes--;
                case '{' -> llaves++;
                case '}' -> llaves--;
                default -> {
                }
            }
            if (parentesis != 0 || corchetes != 0 || llaves != 0) {
                continue;
            }
            if (i + 1 < texto.length()) {
                String dos = texto.substring(i, i + 2);
                if ("<=".equals(dos) || ">=".equals(dos) || "==".equals(dos) || "!=".equals(dos)) {
                    return i;
                }
            }
            if (ch == '<' || ch == '>') {
                return i;
            }
        }
        return -1;
    }

    private String operadorEn(String texto, int posicion) {
        if (posicion + 1 < texto.length()) {
            String dos = texto.substring(posicion, posicion + 2);
            if ("<=".equals(dos) || ">=".equals(dos) || "==".equals(dos) || "!=".equals(dos)) {
                return dos;
            }
        }
        return String.valueOf(texto.charAt(posicion));
    }

    private List<String> dividirSuma(String texto) {
        List<String> partes = new ArrayList<>();
        int parentesis = 0;
        int corchetes = 0;
        int inicio = 0;
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (ch == '(') {
                parentesis++;
            } else if (ch == ')') {
                parentesis--;
            } else if (ch == '[') {
                corchetes++;
            } else if (ch == ']') {
                corchetes--;
            } else if (ch == '+' && parentesis == 0 && corchetes == 0) {
                partes.add(texto.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        partes.add(texto.substring(inicio).trim());
        return partes;
    }

    private int parentesisQueCierra(String texto, int abre) {
        int nivel = 0;
        for (int i = abre; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (ch == '(') {
                nivel++;
            } else if (ch == ')') {
                nivel--;
                if (nivel == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void dirStack(int offset, String registro) {
        if (offset == 0) {
            ctx.emitir("    mv " + registro + ", s3");
        } else {
            ctx.emitir("    addi " + registro + ", s3, " + offset);
        }
        ctx.emitir("    slli " + registro + ", " + registro + ", 3");
        ctx.emitir("    add " + registro + ", s0, " + registro);
    }

    private void cero(String fdest) {
        ctx.emitir("    fcvt.d.w " + fdest + ", zero");
    }
}

