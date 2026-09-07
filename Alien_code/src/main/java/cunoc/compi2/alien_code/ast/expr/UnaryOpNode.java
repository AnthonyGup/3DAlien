package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class UnaryOpNode implements Node {
    public String operador;
    public Node operando;
    private final int line;
    private final int column;

    public UnaryOpNode(String operador, Node operando, int line, int column) {
        this.operador = operador;
        this.operando = operando;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryOp(this);
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