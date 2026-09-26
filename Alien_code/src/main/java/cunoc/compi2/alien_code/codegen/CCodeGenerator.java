package cunoc.compi2.alien_code.codegen;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CCodeGenerator {
    private final StringBuilder code;

    public CCodeGenerator() {
        this.code = new StringBuilder();
    }

    public String generate(List<Cuarteta> instructions, SymbolTable tabla, Map<Integer, String> tiposTemporales) {
        code.setLength(0);
        Map<String, Symbol> simbolos = construirSimbolos(tabla);

        List<DeclararArreglo3D> globalesArreglo = new ArrayList<>();
        List<Cuarteta> cuerpoMain = new ArrayList<>();
        List<FuncionC> funciones = new ArrayList<>();
        FuncionC actual = null;
        boolean hayHalt = false;
        boolean usaConcat = false;

        for (Cuarteta c : instructions) {
            if (c instanceof InicioFuncion3D f) {
                actual = new FuncionC(f);
                funciones.add(actual);
                continue;
            }
            if (c instanceof FinFuncion3D) {
                actual = null;
                continue;
            }
            if (c instanceof DeclararArreglo3D arr) {
                if (actual != null) {
                    actual.arreglos.add(arr);
                } else {
                    globalesArreglo.add(arr);
                }
                continue;
            }
            if (c instanceof Halt3D) {
                hayHalt = true;
                continue;
            }
            if (generaConcat(c)) {
                usaConcat = true;
            }
            if (actual != null) {
                actual.cuerpo.add(c);
            } else {
                cuerpoMain.add(c);
            }
        }

        encabezado(usaConcat);
        estructuras(simbolos);
        if (usaConcat) {
            helpersConcatenacion();
        }
        for (DeclararArreglo3D arr : globalesArreglo) {
            code.append("    ").append(scalarDePalabra(arr.getPalabra())).append(' ')
                    .append(arr.getNombre()).append(dimsTexto(arr.getDims())).append(";\n");
        }
        variablesGlobales(simbolos, tabla);
        code.append('\n');
        for (FuncionC f : funciones) {
            code.append("    ").append(f.firmaDecl()).append(";\n");
        }
        code.append('\n');
        for (FuncionC f : funciones) {
            cuerpoFuncion(f, simbolos, tabla, tiposTemporales);
        }
        cuerpoMain(cuerpoMain, hayHalt, simbolos, tabla, tiposTemporales);
        return code.toString();
    }

    private boolean generaConcat(Cuarteta c) {
        if (c instanceof Llamada3D l) {
            String f = l.getFuncion();
            return "conc".equals(f) || "strn".equals(f) || "strd".equals(f);
        }
        if (c instanceof Operacion3D o) {
            String op = o.operator();
            return "conc".equals(op) || "strn".equals(op) || "strd".equals(op);
        }
        return false;
    }

    private void encabezado(boolean usaConcat) {
        code.append("#include <stdio.h>\n");
        code.append("#include <stdlib.h>\n");
        if (usaConcat) {
            code.append("#include <string.h>\n");
        }
        code.append('\n');
    }

    private void estructuras(Map<String, Symbol> simbolos) {
        for (Symbol s : simbolos.values()) {
            if (esEstructura(s)) {
                code.append("typedef struct ").append(s.getName()).append(' ').append(s.getName()).append(";\n");
            }
        }
        code.append('\n');
        for (Symbol s : simbolos.values()) {
            if (!esEstructura(s)) {
                continue;
            }
            code.append("struct ").append(s.getName()).append(" {\n");
            if (s.getMiembros() != null) {
                for (Symbol m : s.getMiembros().getTodos()) {
                    if (m.isField()) {
                        code.append("    ").append(tipoVar(m)).append(' ').append(m.getName()).append(";\n");
                    }
                }
            }
            code.append("};\n");
        }
        if (!simbolos.isEmpty()) {
            code.append('\n');
        }
    }

    private boolean esEstructura(Symbol s) {
        return s.getKind() == Symbol.Kind.ESTRUCTURA || s.getKind() == Symbol.Kind.CLASE;
    }

    private void helpersConcatenacion() {
        code.append("char* conc(const char* a, const char* b) {\n");
        code.append("    int la = (int)strlen(a);\n");
        code.append("    int lb = (int)strlen(b);\n");
        code.append("    char* r = (char*)malloc(la + lb + 1);\n");
        code.append("    if (r) { memcpy(r, a, la); memcpy(r + la, b, lb + 1); }\n");
        code.append("    return r;\n");
        code.append("}\n\n");
    }

    private void variablesGlobales(Map<String, Symbol> simbolos, SymbolTable tabla) {
        for (Symbol s : simbolos.values()) {
            if (s.getKind() != Symbol.Kind.VARIABLE || s.isArray()) {
                continue;
            }
            if (tabla.resolve(s.getName()) == null) {
                continue;
            }
            code.append("    ").append(tipoVar(s)).append(' ').append(s.getName()).append(";\n");
        }
    }

    private void cuerpoFuncion(FuncionC f, Map<String, Symbol> simbolos, SymbolTable tabla,
            Map<Integer, String> tiposTemporales) {
        code.append(f.firmaDef()).append(" {\n");
        for (DeclararArreglo3D arr : f.arreglos) {
            code.append("    ").append(scalarDePalabra(arr.getPalabra())).append(' ')
                    .append(arr.getNombre()).append(dimsTexto(arr.getDims())).append(";\n");
        }
        Set<String> arreglosMarcados = new LinkedHashSet<>();
        for (DeclararArreglo3D arr : f.arreglos) {
            arreglosMarcados.add(arr.getNombre());
        }
        emitirDeclaraciones(f.cuerpo, f.parametrosNombres, arreglosMarcados, simbolos, tabla, tiposTemporales);
        for (Cuarteta c : f.cuerpo) {
            c.toCCode(code);
            code.append('\n');
        }
        code.append("}\n\n");
    }

    private void emitirDeclaraciones(List<Cuarteta> cuerpo, Set<String> reservados,
            Set<String> arreglosMarcados, Map<String, Symbol> simbolos, SymbolTable tabla,
            Map<Integer, String> tiposTemporales) {
        Set<String> declaradas = new LinkedHashSet<>();
        StringBuilder texto = new StringBuilder();
        for (Cuarteta c : cuerpo) {
            texto.setLength(0);
            c.toCCode(texto);
            coleccionarTokens(texto.toString(), reservados, declaradas);
        }
        for (String nombre : declaradas) {
            if (arreglosMarcados.contains(nombre)) {
                continue;
            }
            String tipo = tipoDe(nombre, simbolos, tabla, tiposTemporales);
            if (tipo == null) {
                continue;
            }
            code.append("    ").append(tipo).append(' ').append(nombre).append(";\n");
        }
    }

    private String tipoDe(String nombre, Map<String, Symbol> simbolos, SymbolTable tabla,
            Map<Integer, String> tiposTemporales) {
        if (nombre.matches("t\\d+")) {
            Integer indice = Integer.valueOf(nombre.substring(1));
            String tipo = tiposTemporales.get(indice);
            return tipo != null ? tipo : "int";
        }
        Symbol s = simbolos.get(nombre);
        if (s == null || s.getKind() != Symbol.Kind.VARIABLE || s.isField()) {
            return null;
        }
        if (tabla.resolve(nombre) != null) {
            return null;
        }
        return tipoVar(s);
    }

    private void cuerpoMain(List<Cuarteta> cuerpoMain, boolean hayHalt,
            Map<String, Symbol> simbolos, SymbolTable tabla, Map<Integer, String> tiposTemporales) {
        code.append("int main(void) {\n");
        emitirDeclaraciones(cuerpoMain, java.util.Collections.emptySet(),
                java.util.Collections.emptySet(), simbolos, tabla, tiposTemporales);
        if (hayHalt) {
            for (Cuarteta c : cuerpoMain) {
                c.toCCode(code);
                code.append('\n');
            }
        }
        code.append("    return 0;\n");
        code.append("}\n");
    }

    private Map<String, Symbol> construirSimbolos(SymbolTable tabla) {
        Map<String, Symbol> mapa = new LinkedHashMap<>();
        for (Symbol s : tabla.listarSimbolos()) {
            mapa.putIfAbsent(s.getName(), s);
        }
        return mapa;
    }

    private void coleccionarTokens(String texto, Set<String> parametros, Set<String> destino) {
        if (texto == null) {
            return;
        }
        StringBuilder tok = new StringBuilder();
        boolean trasCampo = false;
        char anterior = ' ';
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                if (!trasCampo) {
                    tok.append(c);
                }
            } else {
                agregarToken(tok, parametros, destino);
                if (c == '>' && anterior == '-') {
                    trasCampo = true;
                } else {
                    trasCampo = false;
                }
            }
            anterior = c;
        }
        agregarToken(tok, parametros, destino);
    }

    private void agregarToken(StringBuilder tok, Set<String> parametros, Set<String> destino) {
        if (tok.length() == 0) {
            return;
        }
        String t = tok.toString();
        tok.setLength(0);
        if (t.isEmpty() || Character.isDigit(t.charAt(0))) {
            return;
        }
        if (parametros.contains(t)) {
            return;
        }
        if (destino.contains(t)) {
            return;
        }
        destino.add(t);
    }

    private String tipoVar(Symbol s) {
        if (s.isArray()) {
            int estrellas = Math.max(1, s.getDimensiones());
            return tipoCSimbolo(s.getType(), s.getTipoNombre()) + "*".repeat(estrellas);
        }
        return tipoCSimbolo(s.getType(), s.getTipoNombre());
    }

    private String tipoCSimbolo(Type t, String tipoNombre) {
        switch (t) {
            case INT: case BOOL: return "int";
            case FLOAT: return "double";
            case CHAR: return "char";
            case STRING: return "char*";
            case STRUCT: case CLASS: return "struct " + (tipoNombre != null ? tipoNombre : "X") + " *";
            case VOID: return "void";
            default: return "void*";
        }
    }

    private String scalarDePalabra(String palabra) {
        String w = palabra == null ? "" : palabra.toUpperCase();
        switch (w) {
            case "INT": case "BOOL": return "int";
            case "FLOAT": return "double";
            case "CHAR": return "char";
            case "STRING": return "char*";
            case "VOID": return "void";
            default:
                return palabra == null || palabra.isEmpty() ? "int" : "struct " + palabra + " *";
        }
    }

    private String dimsTexto(String dims) {
        if (dims == null || dims.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] partes = dims.split(",");
        for (String p : partes) {
            sb.append('[').append(p.trim()).append(']');
        }
        return sb.toString();
    }

    private static String scalarC(String word) {
        switch (word.toLowerCase()) {
            case "int": case "bool": return "int";
            case "double": return "double";
            case "char": return "char";
            case "string": return "char*";
            case "void": return "void";
            default: return "struct " + word + " *";
        }
    }

    private static final class FuncionC {
        final String nombre;
        final String retorno;
        final List<String[]> parametros;
        final Set<String> parametrosNombres;
        final List<Cuarteta> cuerpo;
        final List<DeclararArreglo3D> arreglos;

        FuncionC(InicioFuncion3D marcador) {
            this.nombre = marcador.getNombre();
            this.retorno = marcador.getRetorno() == null ? "void" : marcador.getRetorno();
            this.parametros = new ArrayList<>();
            this.parametrosNombres = new LinkedHashSet<>();
            this.cuerpo = new ArrayList<>();
            this.arreglos = new ArrayList<>();
            String descripcion = marcador.getDescripcion();
            if (descripcion != null) {
                for (String parte : descripcion.split(";")) {
                    String p = parte.trim();
                    if (p.isEmpty()) {
                        continue;
                    }
                    String nombreParam = p;
                    String tipoParam = "int";
                    int dosPuntos = p.indexOf(':');
                    if (dosPuntos >= 0) {
                        nombreParam = p.substring(0, dosPuntos).trim();
                        tipoParam = p.substring(dosPuntos + 1).trim();
                    }
                    parametros.add(new String[]{nombreParam, tipoParam});
                    parametrosNombres.add(nombreParam);
                }
            }
        }

        String firmaDecl() {
            return retorno + " " + nombre + "(" + listaParams() + ")";
        }

        String firmaDef() {
            return retorno + " " + nombre + "(" + listaParams() + ")";
        }

        private String listaParams() {
            StringBuilder sb = new StringBuilder();
            for (String[] p : parametros) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(cTipoParametro(p[1])).append(' ').append(p[0]);
            }
            return sb.toString();
        }

        private String cTipoParametro(String tipoText) {
            if (tipoText.startsWith("self:")) {
                return "struct " + tipoText.substring(5).trim() + " *";
            }
            if (tipoText.endsWith("[]")) {
                String base = tipoText.substring(0, tipoText.length() - 2).trim();
                return scalarC(base) + "*";
            }
            return scalarC(tipoText);
        }
    }

}