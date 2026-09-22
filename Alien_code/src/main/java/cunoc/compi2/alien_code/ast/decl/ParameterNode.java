package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

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

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitParameter(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }

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
