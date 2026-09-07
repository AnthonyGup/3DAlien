package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

import java.util.List;

public class PrintNode implements Node {
    public List<Node> expresiones;
    private final int line;
    private final int column;

    public PrintNode(List<Node> expresiones, int line, int column) {
        this.expresiones = expresiones;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPrint(this);
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