package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.access.TemporalAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Retornar3D;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.ir.NombresInternos;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ContextoRiscV {
    private final StringBuilder datos = new StringBuilder();
    private final StringBuilder texto = new StringBuilder();
    private final List<String> errores = new ArrayList<>();

    private final List<Cuarteta> todas;
    private final SymbolTable tabla;
    private final Map<Integer, String> tiposTemporales;

    private final Map<String, Symbol> simbolosPorNombre = new LinkedHashMap<>();
    private final LayoutsRiscV layoutsRiscV;

    private final Map<MemoryAccess, TipoRV> tipoRV = new IdentityHashMap<>();

    private MarcoRiscV marcoActual;
    private List<Cuarteta> cuerpoActual;

    private int totalGlobalesBytes;

    private final Map<String, String> etiquetaCadena = new LinkedHashMap<>();
    private final Map<String, String> etiquetaConstante = new LinkedHashMap<>();
    private final Map<String, String> etiquetaFormato = new LinkedHashMap<>();

    private final Set<String> funcionesDefinidas = new java.util.LinkedHashSet<>();
    private final Set<String> llamadasUsuario = new java.util.LinkedHashSet<>();

    private int nextStrLabel = 0;
    private int nextConstLabel = 0;
    private int nextFmtLabel = 0;
    private int nextVirtualSite = 0;
    private int nextErrorBranch = 0;
    private final Map<String, String> mensajesError = new LinkedHashMap<>();

    public ContextoRiscV(List<Cuarteta> todas, SymbolTable tabla, Map<Integer, String> tiposTemporales,
            ClaseLayouts layouts) {
        this.todas = todas == null ? List.of() : todas;
        this.tabla = tabla;
        this.tiposTemporales = tiposTemporales == null ? Map.of() : tiposTemporales;
        this.layoutsRiscV = new LayoutsRiscV(tabla, layouts);
    }

    public void indexar() {
        if (tabla != null) {
            for (Symbol s : tabla.todosLosSimbolos()) {
                simbolosPorNombre.putIfAbsent(s.getName() + "#" + s.getKind(), s);
                simbolosPorNombre.putIfAbsent(s.getName(), s);
            }
            int maxGlobalSlot = -1;
            for (Symbol s : tabla.listarGlobales()) {
                if (s.getKind() == Symbol.Kind.VARIABLE && !s.isField() && s.getOffset() >= 0) {
                    maxGlobalSlot = Math.max(maxGlobalSlot, s.getOffset());
                }
            }
            totalGlobalesBytes = Math.max(16, (maxGlobalSlot + 1) * 8);
        }

        for (Cuarteta c : todas) {
            for (MemoryAccess op : operandosDe(c)) {
                registrarLiteral(op);
            }
        }
    }

    private void registrarLiteral(MemoryAccess op) {
        if (!(op instanceof Literal3D lit)) return;
        String txt = lit.getTexto();
        if (txt == null) return;

        if (lit.getTipo() == Type.STRING && txt.startsWith("\"")) {
            String decoded = decodificarEscapes(txt.substring(1, txt.length() - 1));
            etiquetaCadena.putIfAbsent(decoded, "str_" + nextStrLabel++);
        } else if (txt.indexOf('"') >= 0) {
            int i = 0;
            while (i < txt.length()) {
                int abre = txt.indexOf('"', i);
                if (abre < 0) break;
                int cierra = txt.indexOf('"', abre + 1);
                if (cierra < 0) break;
                String s = txt.substring(abre + 1, cierra);
                String decoded = decodificarEscapes(s);
                etiquetaCadena.putIfAbsent(decoded, "str_" + nextStrLabel++);
                i = cierra + 1;
            }
        }

        if (txt.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            etiquetaConstante.putIfAbsent(txt, "dbl_" + nextConstLabel++);
        }
    }

    public List<MemoryAccess> operandosDe(Cuarteta c) {
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
        } else if (c instanceof LlamadaVirtual3D v) {
            ops.add(v.getReceptor());
            ops.add(v.getDestino());
            ops.addAll(v.getArgumentos());
        } else if (c instanceof Imprimir3D imp && imp.getOperandos() != null) {
            ops.addAll(imp.getOperandos());
        } else if (c instanceof Leer3D r) {
            ops.add(r.getResult());
        } else if (c instanceof Retornar3D r) {
            ops.add(r.getOperand1());
        } else if (c instanceof DeclararArreglo3D) {
        }
        return ops;
    }

    public LayoutsRiscV layoutsRiscV() { return layoutsRiscV; }

    public Symbol simbolo(String nombre) {
        return simbolosPorNombre.get(nombre);
    }

    public String etiquetaFuncion(String nombre) {
        return "Lf_" + NombresInternos.sanitizar(nombre == null ? "" : nombre);
    }

    public StringBuilder datos() { return datos; }
    public StringBuilder texto() { return texto; }
    public List<String> errores() { return errores; }

    public void emitir(String linea) { texto.append(linea).append('\n'); }
    public void emitirDato(String linea) { datos.append(linea).append('\n'); }
    public void error(String mensaje) { errores.add(mensaje); }

    public void emitirErrorSiCero(String registro, String rutina) {
        String continuar = "L_error_branch_" + nextErrorBranch++;
        emitir("    bnez " + registro + ", " + continuar);
        emitirErrorInline(rutina);
        emitir(continuar + ":");
    }

    public void emitirErrorSiNoCero(String registro, String rutina) {
        String continuar = "L_error_branch_" + nextErrorBranch++;
        emitir("    beqz " + registro + ", " + continuar);
        emitirErrorInline(rutina);
        emitir(continuar + ":");
    }

    public void emitirErrorSiNegativo(String registro, String rutina) {
        String continuar = "L_error_branch_" + nextErrorBranch++;
        emitir("    bgez " + registro + ", " + continuar);
        emitirErrorInline(rutina);
        emitir(continuar + ":");
    }

    public void emitirErrorSiMayorOIgual(String izquierda, String limite, String rutina) {
        String continuar = "L_error_branch_" + nextErrorBranch++;
        emitir("    blt " + izquierda + ", " + limite + ", " + continuar);
        emitirErrorInline(rutina);
        emitir(continuar + ":");
    }

    public void emitirErrorInline(String rutina) {
        String etiqueta = mensajesError.get(rutina);
        boolean nuevo = etiqueta == null;
        if (nuevo) {
            etiqueta = "err_msg_" + mensajesError.size();
            mensajesError.put(rutina, etiqueta);
        }
        String texto = switch (rutina) {
            case "error_indice" -> "Error: indice fuera de rango\\n";
            case "error_division" -> "Error: division entre cero\\n";
            case "error_nulo" -> "Error: referencia nula\\n";
            default -> "Error: despacho virtual invalido\\n";
        };
        emitir("    la a0, " + etiqueta);
        emitir("    li a7, 4");
        emitir("    ecall");
        emitir("    li a7, 10");
        emitir("    ecall");
    }

    public Map<String, String> mensajesError() {
        return mensajesError;
    }

    public void setTipoRV(MemoryAccess op, TipoRV t) { tipoRV.put(op, t); }
    public TipoRV getTipoRV(MemoryAccess op) {
        if (op instanceof cunoc.compi2.alien_code.c3d.access.TemporalAccess ta) {
            String ctype = tiposTemporales.get(ta.getNumero());
            if (ctype != null) return TipoRV.desdeString(ctype);
        }
        if (tipoRV.containsKey(op)) return tipoRV.get(op);

        if (op instanceof NameAccess na) {
            if ("self".equals(na.getNombre())) return TipoRV.REF;
            Type t = na.getTipo();
            if (t != null) return TipoRV.desde(t);
            Symbol s = simbolo(na.getNombre());
            if (s != null) return TipoRV.desde(s.getType());
            return TipoRV.REF;
        }
        if (op instanceof Literal3D lit) {
            String txt = lit.getTexto() == null ? "" : lit.getTexto().trim();
            if (txt.startsWith("\"") || "NULL".equals(txt) || "H".equals(txt) || "S".equals(txt)
                    || txt.startsWith("&(") || txt.startsWith("{") || "self".equals(txt)) return TipoRV.REF;
            if (txt.matches("^[+-]?\\d+$")) return TipoRV.ENTERO;
            if (txt.indexOf('.') >= 0 || txt.matches("^[+-]?\\d+[eE][+-]?\\d+$")) return TipoRV.DOBLE;
            return TipoRV.ENTERO;
        }
        if (op instanceof cunoc.compi2.alien_code.c3d.access.CampoAccess ca) {
            return TipoRV.desde(ca.getTipo());
        }
        if (op instanceof cunoc.compi2.alien_code.c3d.access.IndiceAccess ia) {
            return TipoRV.desde(ia.getTipo());
        }
        return TipoRV.ENTERO;
    }

    public void setMarcoActual(MarcoRiscV m) { marcoActual = m; }
    public MarcoRiscV getMarcoActual() { return marcoActual; }
    public int totalGlobalesBytes() { return totalGlobalesBytes; }

    public String etiquetaCadena(String literal) { return etiquetaCadena.get(literal); }
    public String etiquetaConstante(String texto) { return etiquetaConstante.get(texto); }
    public String etiquetaFormato(String texto) { return etiquetaFormato.get(texto); }
    public void setEtiquetaFormato(String texto, String etiqueta) { etiquetaFormato.put(texto, etiqueta); }
    public Map<String, String> etiquetasCadena() { return etiquetaCadena; }
    public Map<String, String> etiquetasConstante() { return etiquetaConstante; }
    public Map<String, String> etiquetasFormato() { return etiquetaFormato; }

    public String nuevaEtiquetaCadena() { return "str_" + nextStrLabel++; }
    public String nuevaEtiquetaConstante() { return "dbl_" + nextConstLabel++; }
    public String nuevaEtiquetaFormato() { return "fmt_" + nextFmtLabel++; }

    public void setCuerpoActual(List<Cuarteta> cuerpo) { cuerpoActual = cuerpo; }
    public List<Cuarteta> getCuerpoActual() { return cuerpoActual; }

    public int globalByte(int slot) { return slot * 8; }

    public int slotByte(int slot) {
        return -(12 + 8 * slot);
    }

    public String registrarCadena(String texto) {
        return etiquetaCadena.computeIfAbsent(texto, k -> "str_" + nextStrLabel++);
    }

    public String registrarConstante(String texto) {
        return etiquetaConstante.computeIfAbsent(texto, k -> "dbl_" + nextConstLabel++);
    }

    public String registrarFormato(String texto) {
        return etiquetaFormato.computeIfAbsent(texto, k -> "fmt_" + nextFmtLabel++);
    }

    public TipoRV campoTipoDe(String tipoNombre, int idx) {
        return layoutsRiscV.tipoCampo(tipoNombre, idx);
    }

    public NameAccess accesoDeNombre(String nombre) {
        if (cuerpoActual == null || nombre == null) return null;
        for (Cuarteta c : cuerpoActual) {
            for (MemoryAccess op : operandosDe(c)) {
                NameAccess na = buscarNombre(op, nombre);
                if (na != null) return na;
            }
        }
        return null;
    }

    public int limiteArreglo(MemoryAccess base) {
        NameAccess nombre = primerNombre(base);
        String buscado = nombre == null ? null : nombre.getNombre();
        if (buscado == null) return -1;
        List<Cuarteta> candidatos = cuerpoActual == null || cuerpoActual.isEmpty() ? todas : cuerpoActual;
        int limite = buscarLimite(candidatos, buscado);
        if (limite >= 0) return limite;
        if (candidatos != todas) return buscarLimite(todas, buscado);
        return -1;
    }

    private int buscarLimite(List<Cuarteta> candidatos, String buscado) {
        Map<Integer, Integer> limitesTemporales = new LinkedHashMap<>();
        for (Cuarteta c : candidatos) {
            if (c instanceof Asignacion3D asignacion
                    && asignacion.getResult() instanceof TemporalAccess temporal
                    && asignacion.getOperand1() instanceof Literal3D literal) {
                Integer limite = limiteMalloc(literal.getTexto());
                if (limite != null) limitesTemporales.put(temporal.getNumero(), limite);
            }
        }
        for (Cuarteta c : candidatos) {
            if (c instanceof DeclararArreglo3D arreglo && buscado.equals(arreglo.getNombre())) {
                String dims = arreglo.getDims();
                if (dims == null) return -1;
                String primero = dims.split(",")[0].trim();
                try {
                    return Integer.parseInt(primero);
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
            if (c instanceof Asignacion3D asignacion
                    && asignacion.getResult() instanceof NameAccess na
                    && buscado.equals(na.getNombre())
                    && asignacion.getOperand1() instanceof TemporalAccess temporal) {
                Integer limite = limitesTemporales.get(temporal.getNumero());
                if (limite != null) return limite;
            }
        }
        return -1;
    }

    private Integer limiteMalloc(String texto) {
        if (texto == null || !texto.startsWith("malloc(")) return null;
        String cuenta = texto.substring("malloc(".length(), texto.length() - 1)
                .split("\\s*\\*")[0].trim();
        try {
            return Integer.parseInt(cuenta);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private NameAccess primerNombre(MemoryAccess op) {
        if (op instanceof NameAccess na && !na.isAutoreferencia()) return na;
        if (op instanceof cunoc.compi2.alien_code.c3d.access.CampoAccess ca) {
            return primerNombre(ca.getBase());
        }
        if (op instanceof cunoc.compi2.alien_code.c3d.access.IndiceAccess ia) {
            NameAccess base = primerNombre(ia.getBase());
            return base != null ? base : primerNombre(ia.getIndice());
        }
        return null;
    }

    private NameAccess buscarNombre(MemoryAccess op, String nombre) {
        if (op instanceof NameAccess na && nombre.equals(na.getNombre()) && !na.isAutoreferencia()) {
            return na;
        }
        if (op instanceof cunoc.compi2.alien_code.c3d.access.CampoAccess ca) {
            return buscarNombre(ca.getBase(), nombre);
        }
        if (op instanceof cunoc.compi2.alien_code.c3d.access.IndiceAccess ia) {
            NameAccess r = buscarNombre(ia.getBase(), nombre);
            return r != null ? r : buscarNombre(ia.getIndice(), nombre);
        }
        return null;
    }

    public int siguienteSitioVirtual() {
        return nextVirtualSite++;
    }

    public int siguienteSitioInline() {
        return nextVirtualSite++;
    }

    public void registrarFuncion(String nombre) {
        funcionesDefinidas.add(nombre);
    }

    public void registrarLlamadaUsuario(String nombre) {
        llamadasUsuario.add(nombre);
    }

    public Set<String> getFuncionesDefinidas() { return funcionesDefinidas; }
    public Set<String> getLlamadasUsuario() { return llamadasUsuario; }

    public static String decodificarEscapes(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                out.append(c);
                continue;
            }
            char e = s.charAt(++i);
            switch (e) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '\'' -> out.append('\'');
                case '0' -> out.append('\0');
                case 'x' -> {
                    if (i + 2 < s.length()) {
                        int val = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                        out.append((char) val);
                        i += 2;
                    } else {
                        out.append('\\').append('x');
                    }
                }
                default -> { out.append('\\').append(e); }
            }
        }
        return out.toString();
    }

    public static byte[] codificarUTF8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}