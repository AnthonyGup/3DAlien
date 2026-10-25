package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextoRiscV {
    private final StringBuilder datos = new StringBuilder();
    private final StringBuilder texto = new StringBuilder();
    private final List<String> errores = new ArrayList<>();

    private final List<Cuarteta> todas;
    private final SymbolTable tabla;
    private final Map<Integer, String> tiposTemporales;
    private final ClaseLayouts layouts;
    private final Map<String, Symbol> simbolosPorNombre = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> camposPorTipo = new HashMap<>();
    private final Map<Llamada3D, Integer> retIds = new IdentityHashMap<>();
    private final Map<LlamadaVirtual3D, Integer> retIdsVirt = new IdentityHashMap<>();
    private int retCounter;
    private int totalGlobalesBytes;

    // Nuevo: offset en bytes para cada operando
    private final Map<MemoryAccess, Integer> offsetBytes = new IdentityHashMap<>();
    // Nuevo: tipo RV para cada operando
    private final Map<MemoryAccess, TipoRV> tipoRV = new IdentityHashMap<>();

    // Marcos por función
    private final Map<String, MarcoRiscV> marcos = new LinkedHashMap<>();
    private MarcoRiscV marcoActual;

    // Offsets de globales en bytes
    private final Map<String, Integer> offsetGlobalBytes = new LinkedHashMap<>();

    // Etiquetas de cadena
    private final Map<String, String> etiquetaCadena = new LinkedHashMap<>();
    private int nextStrLabel = 0;

    // Offsets de constantes double
    private final Map<String, String> etiquetaConstante = new LinkedHashMap<>();
    private int nextConstLabel = 0;

    // Offsets de formatos
    private final Map<String, String> etiquetaFormato = new LinkedHashMap<>();
    private int nextFmtLabel = 0;

    public ContextoRiscV(List<Cuarteta> todas, SymbolTable tabla, Map<Integer, String> tiposTemporales,
            ClaseLayouts layouts) {
        this.todas = todas == null ? List.of() : todas;
        this.tabla = tabla;
        this.tiposTemporales = tiposTemporales == null ? Map.of() : tiposTemporales;
        this.layouts = layouts;
    }

    public void indexar() {
        // 1. Indexar símbolos globales y calcular offsets en bytes
        if (tabla != null) {
            for (Symbol s : tabla.todosLosSimbolos()) {
                simbolosPorNombre.putIfAbsent(s.getName() + "#" + s.getKind(), s);
                simbolosPorNombre.putIfAbsent(s.getName(), s);
            }
            int cursor = 0;
            for (Symbol s : tabla.listarGlobales()) {
                if (s.getKind() == Symbol.Kind.VARIABLE && !s.isField() && s.getOffset() >= 0) {
                    TipoRV t = TipoRV.desde(s.getType());
                    cursor = OffsetsBytes.alinear(cursor, t.alineacion);
                    offsetGlobalBytes.put(s.getName(), cursor);
                    cursor += t.tamano;
                }
            }
            totalGlobalesBytes = (cursor + 15) & ~15; // alinear a 16
        }

        // 2. Recoger literales de cadena, constantes y formatos de todas las cuartetas
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

        // Cadenas
        if (lit.getTipo() == Type.STRING && txt.startsWith("\"")) {
            etiquetaCadena.putIfAbsent(txt, "str_" + nextStrLabel++);
        } else if (txt.indexOf('"') >= 0) {
            int i = 0;
            while (i < txt.length()) {
                int abre = txt.indexOf('"', i);
                if (abre < 0) break;
                int cierra = txt.indexOf('"', abre + 1);
                if (cierra < 0) break;
                String s = txt.substring(abre, cierra + 1);
                etiquetaCadena.putIfAbsent(s, "str_" + nextStrLabel++);
                i = cierra + 1;
            }
        }

        // Constantes double (decimal o científica)
        if (txt.matches("^[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?$")) {
            etiquetaConstante.putIfAbsent(txt, "dbl_" + nextConstLabel++);
        }
    }

    public void asignarRetIds() {
        for (Cuarteta c : todas) {
            if (c instanceof Llamada3D lam && lam.isUsuario()) {
                retIds.put(lam, ++retCounter);
            } else if (c instanceof LlamadaVirtual3D virt) {
                retIdsVirt.put(virt, ++retCounter);
            }
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
        }
        return ops;
    }

    public int camposDe(String tipo) {
        if (tipo == null) return 0;
        Map<String, Integer> mapa = camposPorTipo.computeIfAbsent(tipo, this::construirCampos);
        return mapa.size();
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
        if (tabla == null) return null;
        for (Symbol s : tabla.listarGlobales()) {
            if ((s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA)
                    && s.getName().equals(nombre)) {
                return s;
            }
        }
        return null;
    }

    public Symbol simbolo(String nombre) {
        return simbolosPorNombre.get(nombre);
    }

    public String etiquetaFuncion(String nombre) {
        return "Lf_" + NombresInternos.sanitizar(nombre == null ? "" : nombre);
    }

    // Getters nuevos
    public int totalGlobalesBytes() { return totalGlobalesBytes; }
    public Map<Integer, String> tiposTemporales() { return tiposTemporales; }
    public ClaseLayouts layouts() { return layouts; }
    public Map<Llamada3D, Integer> retIds() { return retIds; }
    public Map<LlamadaVirtual3D, Integer> retIdsVirt() { return retIdsVirt; }
    public StringBuilder datos() { return datos; }
    public StringBuilder texto() { return texto; }
    public List<String> errores() { return errores; }

    public void emitir(String linea) { texto.append(linea).append('\n'); }
    public void emitirDato(String linea) { datos.append(linea).append('\n'); }
    public void error(String mensaje) { errores.add(mensaje); }

    // Offsets y tipos por operando
    public void setOffset(MemoryAccess op, int off) { offsetBytes.put(op, off); }
    public int getOffset(MemoryAccess op) { return offsetBytes.getOrDefault(op, -1); }
    public void setTipoRV(MemoryAccess op, TipoRV t) { tipoRV.put(op, t); }
    public TipoRV getTipoRV(MemoryAccess op) {
        if (op instanceof cunoc.compi2.alien_code.c3d.access.TemporalAccess ta) {
            String ctype = tiposTemporales.get(ta.getNumero());
            return TipoRV.desdeString(ctype);
        }
        return tipoRV.getOrDefault(op, TipoRV.ENTERO);
    }

    // Marco actual
    public void setMarcoActual(MarcoRiscV m) { marcoActual = m; }
    public MarcoRiscV getMarcoActual() { return marcoActual; }
    public Map<String, MarcoRiscV> marcos() { return marcos; }

    // Globales
    public int getOffsetGlobal(String nombre) { return offsetGlobalBytes.getOrDefault(nombre, -1); }

    // Cadenas/constantes/formatos
    public String etiquetaCadena(String literal) { return etiquetaCadena.get(literal); }
    public String etiquetaConstante(String texto) { return etiquetaConstante.get(texto); }
    public String etiquetaFormato(String texto) { return etiquetaFormato.get(texto); }
    public void setEtiquetaFormato(String texto, String etiqueta) { etiquetaFormato.put(texto, etiqueta); }
    public Map<String, String> etiquetasCadena() { return etiquetaCadena; }
    public Map<String, String> etiquetasConstante() { return etiquetaConstante; }
    public Map<String, String> etiquetasFormato() { return etiquetaFormato; }

    // Allocation helpers para strings
    public String nuevaEtiquetaCadena() { return "str_" + nextStrLabel++; }
    public String nuevaEtiquetaConstante() { return "dbl_" + nextConstLabel++; }
    public String nuevaEtiquetaFormato() { return "fmt_" + nextFmtLabel++; }
}