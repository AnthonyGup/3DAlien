package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.expr.AccessNode;

public class IncrementNode implements Node {
    public AccessNode objetivo;
    private final int line;
    private final int column;

    public IncrementNode(AccessNode objetivo, int line, int column) {
        this.objetivo = objetivo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIncrement(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }
}