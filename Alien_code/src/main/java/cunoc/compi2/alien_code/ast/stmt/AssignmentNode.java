package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.expr.AccessNode;

public class AssignmentNode implements Node {
    public AccessNode destino;
    public Node valor;
    private final int line;
    private final int column;

    public AssignmentNode(AccessNode destino, Node valor, int line, int column) {
        this.destino = destino;
        this.valor = valor;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignment(this);
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