package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class ElseIfNode implements Node {
    public Node condicion;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ElseIfNode(Node condicion, BlockNode cuerpo, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    public boolean esElse() {
        return condicion == null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitElseIf(this);
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