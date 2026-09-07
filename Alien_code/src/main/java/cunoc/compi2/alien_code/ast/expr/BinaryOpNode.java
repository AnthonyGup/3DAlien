package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class BinaryOpNode implements Node {
    public String operador;
    public Node izquierda;
    public Node derecha;
    private final int line;
    private final int column;

    public BinaryOpNode(String operador, Node izquierda, Node derecha, int line, int column) {
        this.operador = operador;
        this.izquierda = izquierda;
        this.derecha = derecha;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryOp(this);
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