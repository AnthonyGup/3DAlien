package cunoc.compi2.alien_code.codegen;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.CampoAccess;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.access.TemporalAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Retornar3D;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generador de codigo C de tres direcciones puro.
 *
 * Produce un unico archivo con un unico main(), sin structs ni funciones de
 * usuario. Toda la memoria vive en Stack[]/Heap[]/Str[] y el flujo se controla
 * solo con goto/etiquetas. Se permite usar la biblioteca estandar (printf,
 * scanf, strcpy, strlen, sprintf) como capa de entrada/salida.
 */
public class CCodeGenerator {
    private static final int STACK_SIZE = 1 << 20;
    private static final int HEAP_SIZE = 1 << 22;
    private static final int STR_SIZE = 1 << 20;

    private final StringBuilder code = new StringBuilder();
    private Map<Integer, String> tiposTemporales = new HashMap<>();
    private SymbolTable tabla;

    private final Map<String, Map<String, Integer>> camposPorTipo = new HashMap<>();
    private final Map<String, Symbol> simbolosPorNombre = new LinkedHashMap<>();
    private final Map<String, Integer> stringBases = new LinkedHashMap<>();
    private int areaStrings = 0;
    private int totalGlobales = 0;

    private final Map<Llamada3D, Integer> retIds = new IdentityHashMap<>();
    private final Map<cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D, Integer> retIdsVirt = new IdentityHashMap<>();
    private int retCounter = 0;
    private List<Cuarteta> todas = new ArrayList<>();
    private ClaseLayouts layouts;

    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales) {
        return generate(instrucciones, tabla, tiposTemporales, null);
    }

    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales,
            ClaseLayouts layouts) {
        code.setLength(0);
        this.tiposTemporales = tiposTemporales == null ? new HashMap<>() : tiposTemporales;
        this.tabla = tabla;
        this.todas = instrucciones;
        this.layouts = layouts;
        camposPorTipo.clear();
        simbolosPorNombre.clear();
        stringBases.clear();
        retIds.clear();
        retIdsVirt.clear();
        retCounter = 0;
        totalGlobales = 0;
        areaStrings = 0;

        construirIndices(instrucciones);
        asignarRetIds(instrucciones);

        List<Cuarteta> topLevel = new ArrayList<>();
        List<int[]> bloques = new ArrayList<>();
        List<InicioFuncion3D> marcadores = new ArrayList<>();
        particionar(instrucciones, topLevel, bloques, marcadores);

        emitirPreambulo();
        emitirCuerpo(topLevel, bloques, marcadores);
        emitirCierre();

        return code.toString();
    }

    // ------------------------------------------------------------------
    // Indices auxiliares
    // ------------------------------------------------------------------
    private void construirIndices(List<Cuarteta> instrucciones) {
        if (tabla != null) {
            for (Symbol s : tabla.todosLosSimbolos()) {
                simbolosPorNombre.putIfAbsent(s.getName() + "#" + s.getKind(), s);
                simbolosPorNombre.putIfAbsent(s.getName(), s);
            }
            Set<String> tipos = new LinkedHashSet<>();
            for (Symbol s : tabla.listarGlobales()) {
                if (s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA) {
                    tipos.add(s.getName());
                }
                if (s.getKind() == Symbol.Kind.VARIABLE && !s.isField() && s.getOffset() >= 0) {
                    totalGlobales = Math.max(totalGlobales, s.getOffset() + 1);
                }
            }
            for (String t : tipos) {
                camposPorTipo.put(t, construirCampos(t));
            }
        }
        for (Cuarteta c : instrucciones) {
            for (MemoryAccess op : operandosDe(c)) {
                registrarLiteralString(op);
            }
        }
    }

    private Map<String, Integer> construirCampos(String tipo) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        Symbol ts = buscarTipo(tipo);
        int i = 0;
        if (ts != null && ts.getMiembros() != null) {
            for (Symbol f : ts.getMiembros().getTodos()) {
                if (f.getKind() == Symbol.Kind.VARIABLE) {
                    mapa.put(f.getName(), i++);
                }
            }
        }
        return mapa;
    }

    private Symbol buscarTipo(String nombre) {
        if (tabla == null) {
            return null;
        }
        for (Symbol s : tabla.listarGlobales()) {
            if ((s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA)
                    && s.getName().equals(nombre)) {
                return s;
            }
        }
        return null;
    }

    private int cuentaCampos(String tipo) {
        Map<String, Integer> m = camposPorTipo.computeIfAbsent(tipo, this::construirCampos);
        return m.size();
    }

    private int offsetCampo(String tipo, String campo) {
        if (tipo == null) {
            return 0;
        }
        Map<String, Integer> m = camposPorTipo.computeIfAbsent(tipo, this::construirCampos);
        Integer o = m.get(campo);
        return o == null ? 0 : o;
    }

    private void registrarLiteralString(MemoryAccess op) {
        if (op instanceof Literal3D lit && lit.getTipo() == Type.STRING) {
            String texto = lit.getTexto();
            if (texto == null) {
                return;
            }
            if (texto.startsWith("\"") && !stringBases.containsKey(texto)) {
                registrarCadena(texto);
            } else if (texto.indexOf('"') >= 0) {
                registrarCadenasEmbebidas(texto);
            }
        }
    }

    private void registrarCadena(String texto) {
        if (texto == null || !texto.startsWith("\"") || stringBases.containsKey(texto)) {
            return;
        }
        stringBases.put(texto, areaStrings);
        int contenido = Math.max(0, texto.length() - 2);
        areaStrings += contenido + 1;
    }

    private void registrarCadenasEmbebidas(String texto) {
        int i = 0;
        while (i < texto.length()) {
            int abre = texto.indexOf('"', i);
            if (abre < 0) {
                break;
            }
            int cierra = texto.indexOf('"', abre + 1);
            if (cierra < 0) {
                break;
            }
            registrarCadena(texto.substring(abre, cierra + 1));
            i = cierra + 1;
        }
    }

    private Iterable<MemoryAccess> operandosDe(Cuarteta c) {
        List<MemoryAccess> ops = new ArrayList<>();
        if (c instanceof Asignacion3D a) {
            ops.add(a.getResult());
            ops.add(a.getOperand1());
        } else if (c instanceof Operacion3D o) {
            ops.add(o.getOperand1());
            ops.add(o.getOperand2());
            ops.add(o.getResult());
        } else if (c instanceof Condicional3D cond) {
            ops.add(cond.getOperand1());
        } else if (c instanceof Llamada3D l) {
            ops.add(l.getResult());
            ops.addAll(l.getArgumentos());
        } else if (c instanceof cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D v) {
            ops.add(v.getReceptor());
            ops.add(v.getDestino());
            ops.addAll(v.getArgumentos());
        } else if (c instanceof Imprimir3D imp && imp.getOperandos() != null) {
            ops.addAll(imp.getOperandos());
        } else if (c instanceof Leer3D r) {
            ops.add(r.getResult());
        } else if (c instanceof Retornar3D r) {
            ops.add(r.getOperand1());
        }
        return ops;
    }

    private void asignarRetIds(List<Cuarteta> instrucciones) {
        for (Cuarteta c : instrucciones) {
            if (c instanceof Llamada3D lam && lam.isUsuario()) {
                retIds.put(lam, ++retCounter);
            } else if (c instanceof cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D virt) {
                retIdsVirt.put(virt, ++retCounter);
            }
        }
    }

    private void particionar(List<Cuarteta> instrucciones, List<Cuarteta> topLevel,
            List<int[]> bloques, List<InicioFuncion3D> marcadores) {
        int i = 0;
        while (i < instrucciones.size()) {
            Cuarteta c = instrucciones.get(i);
            if (c instanceof InicioFuncion3D ini) {
                int inicio = i + 1;
                int fin = inicio;
                while (fin < instrucciones.size() && !(instrucciones.get(fin) instanceof FinFuncion3D)) {
                    fin++;
                }
                bloques.add(new int[]{inicio, fin});
                marcadores.add(ini);
                i = fin + 1;
            } else {
                topLevel.add(c);
                i++;
            }
        }
    }

    // ------------------------------------------------------------------
    // Emision
    // ------------------------------------------------------------------
    private void emitirPreambulo() {
        code.append("/* C3D puro: un unico main, Stack[]/Heap[], goto/etiquetas */\n");
        code.append("#include <stdio.h>\n");
        code.append("#include <stdlib.h>\n");
        code.append("#include <string.h>\n\n");
        code.append("static double Stack[").append(STACK_SIZE).append("];\n");
        code.append("static double Heap[").append(HEAP_SIZE).append("];\n");
        code.append("static char Str[").append(STR_SIZE).append("];\n\n");
    }

    private void emitirCuerpo(List<Cuarteta> topLevel, List<int[]> bloques, List<InicioFuncion3D> marcadores) {
        code.append("int main(void) {\n");
        code.append("    setvbuf(stdout, NULL, _IONBF, 0);\n");
        code.append("    double P = ").append(totalGlobales).append(".0;\n");
        code.append("    double H = 1.0;\n");
        code.append("    double S = ").append(areaStrings).append(".0;\n");
        code.append("    int __ri = 0; double __rd = 0.0; char __rc = 0; double __ret = 0.0;")
            .append(retIdsVirt.isEmpty() ? "" : " double __rv = 0.0;").append("\n");
        for (Map.Entry<String, Integer> e : stringBases.entrySet()) {
            code.append("    strcpy(&Str[").append(e.getValue()).append("], ").append(e.getKey()).append(");\n");
        }
        code.append("    goto L_main;\n\n");

        for (int b = 0; b < bloques.size(); b++) {
            InicioFuncion3D ini = marcadores.get(b);
            code.append(etiquetaFuncion(ini.getNombre())).append(": ;\n");
            int[] rango = bloques.get(b);
            for (int k = rango[0]; k < rango[1]; k++) {
                emitirInstruccion(todas.get(k));
            }
            code.append("    goto L_disp;\n\n");
        }

        code.append("L_main: ;\n");
        for (Cuarteta c : topLevel) {
            emitirInstruccion(c);
        }
        code.append("    goto L_disp;\n\n");

        code.append("L_disp: ;\n");
        for (Map.Entry<Llamada3D, Integer> e : retIds.entrySet()) {
            code.append("    if (Stack[(int)(P + 0)] == ").append(e.getValue()).append(") goto Lret_")
                .append(e.getValue()).append(";\n");
        }
        for (Map.Entry<cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D, Integer> e : retIdsVirt.entrySet()) {
            code.append("    if (Stack[(int)(P + 0)] == ").append(e.getValue()).append(") goto Lret_")
                .append(e.getValue()).append(";\n");
        }
        code.append("    exit(0);\n");
    }

    private void emitirCierre() {
        code.append("}\n");
    }

    // ------------------------------------------------------------------
    // Instrucciones
    // ------------------------------------------------------------------
    private void emitirInstruccion(Cuarteta c) {
        if (c instanceof Etiqueta3D et) {
            code.append("    ").append(nombreEtiqueta(et.getResult())).append(": ;\n");
        } else if (c instanceof Goto3D go) {
            code.append("    goto ").append(nombreEtiqueta(go.getResult())).append(";\n");
        } else if (c instanceof Condicional3D cond) {
            String destino = nombreEtiqueta(cond.getResult());
            String condStr = expr(cond.getOperand1());
            if ("if_true".equals(cond.operator())) {
                code.append("    if (").append(condStr).append(") goto ").append(destino).append(";\n");
            } else {
                code.append("    if (!(").append(condStr).append(")) goto ").append(destino).append(";\n");
            }
        } else if (c instanceof Asignacion3D asg) {
            emitirAsignacion(asg);
        } else if (c instanceof Operacion3D op) {
            emitirOperacion(op);
        } else if (c instanceof DeclararArreglo3D arr) {
            emitirDeclararArreglo(arr);
        } else if (c instanceof Imprimir3D imp) {
            emitirImprimir(imp);
        } else if (c instanceof Leer3D leer) {
            emitirLeer(leer);
        } else if (c instanceof Llamada3D lam) {
            emitirLlamada(lam);
        } else if (c instanceof cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D virt) {
            emitirLlamadaVirtual(virt);
        } else if (c instanceof Retornar3D ret) {
            if (ret.getOperand1() != null) {
                code.append("    Stack[(int)(P + 1)] = ").append(expr(ret.getOperand1())).append(";\n");
            }
            code.append("    goto L_disp;\n");
        } else if (c instanceof Halt3D) {
            code.append("    exit(0);\n");
        }
    }

    private void emitirAsignacion(Asignacion3D asg) {
        MemoryAccess destino = asg.getResult();
        MemoryAccess valor = asg.getOperand1();
        String dest = expr(destino);

        if (valor instanceof Literal3D lit) {
            String texto = lit.getTexto() == null ? "" : lit.getTexto();
            if (texto.startsWith("malloc(sizeof(struct ")) {
                String clase = texto.substring("malloc(sizeof(struct ".length());
                int corte = clase.indexOf(')');
                if (corte >= 0) clase = clase.substring(0, corte);
                boolean esClase = layouts != null && layouts.contiene(clase);
                if (esClase) {
                    int tamano = Math.max(1, layouts.tamanoDe(clase));
                    code.append("    ").append(dest).append(" = H;\n");
                    code.append("    H = H + ").append(tamano).append(";\n");
                    for (int k = 0; k < tamano; k++) {
                        code.append("    Heap[(int)(").append(dest).append(") + ").append(k).append("] = 0;\n");
                    }
                    code.append("    Heap[(int)(").append(dest).append(") + 0] = ")
                        .append(layouts.idDe(clase)).append(";\n");
                    return;
                }
                int n = cuentaCampos(clase);
                code.append("    ").append(dest).append(" = H;\n");
                code.append("    H = H + ").append(Math.max(1, n)).append(";\n");
                for (int k = 0; k < Math.max(1, n); k++) {
                    code.append("    Heap[(int)(").append(dest).append(") + ").append(k).append("] = 0;\n");
                }
                return;
            }
            if (texto.startsWith("malloc(")) {
                String interno = extraerInternoMalloc(texto);
                String cuenta = interno;
                int prod = interno.indexOf(" * ");
                if (prod > 0) {
                    cuenta = interno.substring(0, prod);
                }
                code.append("    ").append(dest).append(" = H;\n");
                code.append("    H = H + (").append(cuenta).append(");\n");
                return;
            }
            if (texto.startsWith("{")) {
                emitirLiteralEstructura(dest, destino, texto);
                return;
            }
            if (texto.startsWith("&(struct")) {
                int abreLlave = texto.indexOf('{');
                if (abreLlave >= 0) {
                    emitirLiteralEstructura(dest, destino, texto.substring(abreLlave));
                    return;
                }
            }
            if (texto.startsWith("(")) {
                code.append("    ").append(dest).append(" = 0; /* literal compuesto pendiente */\n");
                return;
            }
        }
        code.append("    ").append(dest).append(" = ").append(expr(valor)).append(";\n");
    }

    private void emitirLiteralEstructura(String dest, MemoryAccess destino, String texto) {
        int abre = texto.indexOf('{');
        int cierra = texto.lastIndexOf('}');
        if (abre < 0 || cierra <= abre) {
            code.append("    ").append(dest).append(" = 0;\n");
            return;
        }
        List<String> valores = dividirNivelSuperior(texto.substring(abre + 1, cierra));
        int n = Math.max(valores.size(), 1);
        code.append("    ").append(dest).append(" = H;\n");
        code.append("    H = H + ").append(n).append(";\n");
        for (int i = 0; i < valores.size(); i++) {
            String v = valores.get(i).trim();
            if (v.isEmpty()) {
                v = "0";
            }
            if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                Integer base = stringBases.get(v);
                v = base == null ? "0" : base.toString();
            }
            code.append("    Heap[(int)(").append(dest).append(") + ").append(i).append("] = (")
                .append(v).append(");\n");
        }
    }

    private List<String> dividirNivelSuperior(String contenido) {
        List<String> partes = new ArrayList<>();
        int profundidad = 0;
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < contenido.length(); i++) {
            char ch = contenido.charAt(i);
            if (ch == '{' || ch == '(' || ch == '[') {
                profundidad++;
            } else if (ch == '}' || ch == ')' || ch == ']') {
                profundidad--;
            }
            if (ch == ',' && profundidad == 0) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        if (actual.length() > 0 || !partes.isEmpty()) {
            partes.add(actual.toString());
        }
        return partes;
    }

    private String extraerInternoMalloc(String texto) {
        int abre = texto.indexOf('(');
        int cierra = texto.lastIndexOf(')');
        if (abre >= 0 && cierra > abre) {
            return texto.substring(abre + 1, cierra);
        }
        return "0";
    }

    private void emitirOperacion(Operacion3D op) {
        String operador = op.operator();
        if ("conc".equals(operador) || "strn".equals(operador) || "strd".equals(operador)) {
            emitirConcatenacion(operador, op);
            return;
        }
        String res = expr(op.getResult());
        String op1 = expr(op.getOperand1());
        String op2 = expr(op.getOperand2());
        if (op.getResult() == null) {
            code.append("    ").append(op1).append(" ").append(operador).append(" ").append(op2).append(";\n");
            return;
        }
        if (op.getOperand2() == null) {
            code.append("    ").append(res).append(" = ").append(operador).append(" ").append(op1).append(";\n");
            return;
        }
        code.append("    ").append(res).append(" = ").append(op1).append(" ").append(operador).append(" ")
            .append(op2).append(";\n");
    }

    private void emitirConcatenacion(String operador, Operacion3D op) {
        String res = expr(op.getResult());
        String a = expr(op.getOperand1());
        String b = expr(op.getOperand2());
        if ("conc".equals(operador)) {
            code.append("    ").append(res).append(" = S;\n");
            code.append("    strcpy(&Str[(int)S], &Str[(int)(").append(a).append(")]);\n");
            code.append("    S = S + strlen(&Str[(int)(").append(a).append(")]);\n");
            code.append("    strcpy(&Str[(int)S], &Str[(int)(").append(b).append(")]);\n");
            code.append("    S = S + strlen(&Str[(int)(").append(b).append(")]) + 1.0;\n");
        } else {
            String fmt = "strd".equals(operador) ? "%d" : "%g";
            String cast = "strd".equals(operador) ? "(int)(" + a + ")" : "(" + a + ")";
            code.append("    ").append(res).append(" = S;\n");
            code.append("    sprintf(&Str[(int)S], \"").append(fmt).append("\", ").append(cast).append(");\n");
            code.append("    S = S + strlen(&Str[(int)S]) + 1.0;\n");
        }
    }

    private void emitirDeclararArreglo(DeclararArreglo3D arr) {
        Symbol s = simbolosPorNombre.get(arr.getNombre());
        int off = s != null ? s.getOffset() : -1;
        if (off < 0) {
            code.append("    /* arreglo sin offset: ").append(arr.getNombre()).append(" */\n");
            return;
        }
        String dest = s.isOffsetGlobal() ? "Stack[(int)" + off + "]" : "Stack[(int)(P + " + off + ")]";
        long total = calcularTotalArreglo(arr.getDims());
        code.append("    ").append(dest).append(" = H;\n");
        code.append("    H = H + ").append(Math.max(1, total)).append(";\n");
    }

    private long calcularTotalArreglo(String dims) {
        if (dims == null || dims.isBlank()) {
            return 1;
        }
        String limpio = dims.replace("[", " ").replace("]", " ");
        long total = 1;
        boolean hay = false;
        for (String parte : limpio.trim().split("\\s+")) {
            if (parte.isEmpty()) continue;
            try {
                total *= Long.parseLong(parte.trim());
                hay = true;
            } catch (NumberFormatException ignored) {
                return 64;
            }
        }
        return hay ? total : 64;
    }

    private void emitirImprimir(Imprimir3D imp) {
        code.append("    printf(").append(imp.getFormato());
        List<MemoryAccess> operandos = imp.getOperandos();
        List<String> especificadores = imp.getEspecificadores();
        if (operandos != null && !operandos.isEmpty()) {
            for (int i = 0; i < operandos.size(); i++) {
                String esp = especificadores != null && i < especificadores.size()
                        ? especificadores.get(i) : "%d";
                code.append(", ").append(argImpresion(operandos.get(i), esp));
            }
        } else if (imp.getArgs() != null && !imp.getArgs().isEmpty()) {
            code.append(", ").append(imp.getArgs());
        }
        code.append(");\n");
    }

    private String argImpresion(MemoryAccess op, String especificadorBruto) {
        String e = expr(op);
        String especificador = especificadorBruto == null ? "%d" : especificadorBruto.trim();
        switch (especificador) {
            case "%s":
                if (esBooleano(op)) {
                    return "(" + e + " ? \"true\" : \"false\")";
                }
                return "&Str[(int)(" + e + ")]";
            case "%c":
                return "(int)(" + e + ")";
            case "%p":
                return "(void*)(long)(" + e + ")";
            case "%d":
                return "(int)(" + e + ")";
            case "%g":
                return "(" + e + ")";
            default:
                return "(" + e + ")";
        }
    }

    private boolean esBooleano(MemoryAccess op) {
        Type t = tipoDe(op);
        return t == Type.BOOL;
    }

    private void emitirLeer(Leer3D leer) {
        String dest = expr(leer.getResult());
        String fmt = leer.getFormato() == null ? "%d" : leer.getFormato().trim();
        if ("%s".equals(fmt)) {
            code.append("    scanf(\"%s\", &Str[(int)(").append(dest).append(")]);\n");
        } else if ("%lf".equals(fmt)) {
            code.append("    scanf(\"%lf\", &__rd); ").append(dest).append(" = __rd;\n");
        } else if ("%c".equals(fmt)) {
            code.append("    scanf(\" %c\", &__rc); ").append(dest).append(" = __rc;\n");
        } else {
            code.append("    scanf(\"%d\", &__ri); ").append(dest).append(" = __ri;\n");
        }
    }

    private void emitirLlamada(Llamada3D lam) {
        String nombre = lam.getFuncion();
        if (!lam.isUsuario()) {
            emitirLlamadaNativa(lam, nombre);
            return;
        }
        int retId = retIds.getOrDefault(lam, 0);
        int frame = Math.max(0, lam.getCallerFrameSize());
        List<MemoryAccess> args = lam.getArgumentos();
        code.append("    Stack[(int)(P + ").append(frame).append(" + 0)] = ").append(retId).append(";\n");
        for (int i = 0; i < args.size(); i++) {
            int slot;
            if (lam.isTieneSelf() && i == 0) {
                slot = 2;
            } else {
                slot = 3 + i - (lam.isTieneSelf() ? 1 : 0);
            }
            code.append("    Stack[(int)(P + ").append(frame).append(" + ").append(slot).append(")] = ")
                .append(expr(args.get(i))).append(";\n");
        }
        code.append("    P = P + ").append(frame).append(".0;\n");
        code.append("    goto ").append(etiquetaFuncion(nombre)).append(";\n");
        code.append("Lret_").append(retId).append(": ;\n");
        if (lam.getResult() != null) {
            code.append("    __ret = Stack[(int)(P + 1)];\n");
        }
        code.append("    P = P - ").append(frame).append(".0;\n");
        if (lam.getResult() != null) {
            code.append("    ").append(expr(lam.getResult())).append(" = __ret;\n");
        }
    }

    private void emitirLlamadaVirtual(cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D virt) {
        int retId = retIdsVirt.getOrDefault(virt, 0);
        int frame = Math.max(0, virt.getCallerFrameSize());
        String receptor = expr(virt.getReceptor());
        List<MemoryAccess> args = virt.getArgumentos();
        code.append("    __rv = ").append(receptor).append(";\n");
        code.append("    Stack[(int)(P + ").append(frame).append(" + 0)] = ").append(retId).append(";\n");
        code.append("    Stack[(int)(P + ").append(frame).append(" + 2)] = __rv;\n");
        for (int i = 0; i < args.size(); i++) {
            code.append("    Stack[(int)(P + ").append(frame).append(" + ").append(3 + i).append(")] = ")
                .append(expr(args.get(i))).append(";\n");
        }
        code.append("    P = P + ").append(frame).append(".0;\n");
        code.append("    switch((int)(Heap[(int)(__rv) + 0])) {\n");
        for (Map.Entry<Integer, String> e : virt.tablaDespacho().entrySet()) {
            code.append("        case ").append(e.getKey()).append(": goto ")
                .append(e.getValue()).append(";\n");
        }
        code.append("    }\n");
        code.append("Lret_").append(retId).append(": ;\n");
        if (virt.getDestino() != null) {
            code.append("    __ret = Stack[(int)(P + 1)];\n");
        }
        code.append("    P = P - ").append(frame).append(".0;\n");
        if (virt.getDestino() != null) {
            code.append("    ").append(expr(virt.getDestino())).append(" = __ret;\n");
        }
    }

    private void emitirLlamadaNativa(Llamada3D lam, String nombre) {
        String res = lam.getResult() != null ? expr(lam.getResult()) : null;
        List<MemoryAccess> args = lam.getArgumentos();
        if ("conc".equals(nombre) || "strd".equals(nombre) || "strn".equals(nombre) || "strc".equals(nombre)) {
            Operacion3D op = new Operacion3D(nombre,
                    args.size() > 0 ? args.get(0) : null,
                    args.size() > 1 ? args.get(1) : null,
                    lam.getResult());
            if ("strc".equals(nombre)) {
                String c = args.isEmpty() ? "0" : expr(args.get(0));
                code.append("    ").append(res).append(" = S;\n");
                code.append("    Str[(int)S] = (char)(").append(c).append(");\n");
                code.append("    Str[(int)S + 1] = 0;\n");
                code.append("    S = S + 2.0;\n");
                return;
            }
            emitirConcatenacion(nombre, op);
            return;
        }
        // Llamada nativa generica (imprimir/leer ya se traducen antes): ignorar.
        if (res != null) {
            code.append("    ").append(res).append(" = 0;\n");
        }
    }

    // ------------------------------------------------------------------
    // Render de operandos
    // ------------------------------------------------------------------
    private String expr(MemoryAccess op) {
        if (op == null) {
            return "0";
        }
        if (op instanceof Literal3D lit) {
            return exprLiteral(lit);
        }
        if (op instanceof NameAccess na) {
            return exprNombre(na);
        }
        if (op instanceof TemporalAccess ta) {
            int off = ta.getOffset();
            if (off < 0) {
                return "t" + ta.getNumero();
            }
            return "Stack[(int)(P + " + off + ")]";
        }
        if (op instanceof CampoAccess ca) {
            return "Heap[(int)(" + expr(ca.getBase()) + ") + " + ca.getOffset() + "]";
        }
        if (op instanceof IndiceAccess ia) {
            return "Heap[(int)(" + expr(ia.getBase()) + " + " + expr(ia.getIndice()) + ")]";
        }
        if (op instanceof LabelAccess la) {
            return la.getNombre();
        }
        StringBuilder sb = new StringBuilder();
        op.toCCode(sb);
        return sb.toString();
    }

    private String exprLiteral(Literal3D lit) {
        String texto = lit.getTexto() == null ? "0" : lit.getTexto();
        if (lit.getTipo() == Type.STRING) {
            if (texto.startsWith("\"")) {
                Integer base = stringBases.get(texto);
                return base == null ? "0" : Integer.toString(base);
            }
            return "0";
        }
        if ("NULL".equals(texto)) {
            return "0";
        }
        if ("self".equals(texto)) {
            return "Stack[(int)(P + 2)]";
        }
        return texto;
    }

    private String exprNombre(NameAccess na) {
        int off = na.getOffset();
        if (na.isAutoreferencia()) {
            return "Heap[(int)(Stack[(int)(P + 2)]) + " + (off < 0 ? 0 : off) + "]";
        }
        if (off < 0) {
            return na.getNombre();
        }
        if (na.isGlobal()) {
            return "Stack[(int)" + off + "]";
        }
        return "Stack[(int)(P + " + off + ")]";
    }

    private Type tipoDe(MemoryAccess op) {
        if (op instanceof TemporalAccess ta) {
            String ctype = tiposTemporales.get(ta.getNumero());
            return tipoDesdeCType(ctype);
        }
        return op == null ? null : op.getTipo();
    }

    private Type tipoDesdeCType(String ctype) {
        if (ctype == null) {
            return Type.INT;
        }
        if (ctype.contains("struct") || ctype.endsWith("*")) {
            return Type.CLASS;
        }
        if (ctype.startsWith("bool")) {
            return Type.BOOL;
        }
        if (ctype.startsWith("double") || ctype.startsWith("float")) {
            return Type.FLOAT;
        }
        if (ctype.startsWith("char") && !ctype.contains("*")) {
            return Type.CHAR;
        }
        if (ctype.contains("char")) {
            return Type.STRING;
        }
        return Type.INT;
    }

    private String nombreEtiqueta(MemoryAccess op) {
        if (op instanceof LabelAccess la) {
            return la.getNombre();
        }
        return "L0";
    }

    private String etiquetaFuncion(String nombre) {
        return "Lf_" + sanitizar(nombre);
    }

    private String sanitizar(String nombre) {
        StringBuilder sb = new StringBuilder();
        for (char ch : nombre.toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
