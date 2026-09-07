package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

import java.util.List;

public class StructLiteralNode implements Node {
    public List<Node> valores;
    private final int line;
    private final int column;

    public StructLiteralNode(List<Node> valores, int line, int column) {
        this.valores = valores;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStructLiteral(this);
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