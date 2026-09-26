package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class ParameterNode implements Node {
    public Type tipo;
    public String nombre;
    public boolean porReferencia;
    public String tipoNombre;
    public Type tipoElemento;
    public int dimensiones = 1;
    private final int line;
    private final int column;

    public ParameterNode(Type tipo, String nombre, boolean porReferencia, String tipoNombre, int line, int column) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.porReferencia = porReferencia;
        this.tipoNombre = tipoNombre;
        this.line = line;
        this.column = column;
    }

    public static String descriptores(List<ParameterNode> parametros) {
        StringBuilder sb = new StringBuilder();
        for (ParameterNode p : parametros) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(p.descriptor());
        }
        return sb.toString();
    }

    public String descriptor() {
        Type baseTipo = tipo == Type.ARRAY ? tipoElemento : tipo;
        String base;
        if (baseTipo == Type.STRUCT || baseTipo == Type.CLASS) {
            base = tipoNombre != null ? tipoNombre : "void";
        } else {
            base = primitivo(baseTipo);
        }
        return nombre + ":" + (tipo == Type.ARRAY ? base + "[]" : base);
    }

    private static String primitivo(Type t) {
        switch (t) {
            case INT: return "int";
            case FLOAT: return "double";
            case BOOL: return "bool";
            case CHAR: return "char";
            case STRING: return "string";
            case VOID: return "void";
            default: return "void";
        }
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (porReferencia && tipo != Type.ARRAY && tipo != Type.STRUCT && tipo != Type.CLASS) {
            ctx.registrarError(getLine(), getColumn(),
                "Solo arreglos ([]) o estructuras ({}) pueden pasarse por referencia, no " + tipo);
        }
        Symbol simbolo = Symbol.variable(nombre, tipo, tipoElemento, tipoNombre,
                dimensiones, true, false, getLine(), getColumn());
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "Parámetro duplicado: '" + nombre + "'");
        }
        return tipo;
    }
}
