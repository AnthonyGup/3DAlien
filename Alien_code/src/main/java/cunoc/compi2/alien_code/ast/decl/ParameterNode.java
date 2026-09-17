package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class ParameterNode implements Node {
    public Type tipo;
    public String nombre;
    public boolean porReferencia;
    private final int line;
    private final int column;

    public ParameterNode(Type tipo, String nombre, boolean porReferencia, int line, int column) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.porReferencia = porReferencia;
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
        return null;
    }
}