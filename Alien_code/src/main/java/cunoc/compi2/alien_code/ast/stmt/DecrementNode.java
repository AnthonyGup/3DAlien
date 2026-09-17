package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.expr.AccessNode;

public class DecrementNode implements Node {
    public AccessNode objetivo;
    private final int line;
    private final int column;

    public DecrementNode(AccessNode objetivo, int line, int column) {
        this.objetivo = objetivo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDecrement(this);
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