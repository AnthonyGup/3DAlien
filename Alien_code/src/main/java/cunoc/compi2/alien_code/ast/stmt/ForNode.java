package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class ForNode implements Node {
    public Node inicial;
    public Node condicion;
    public Node paso;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ForNode(Node inicial, Node condicion, Node paso, BlockNode cuerpo, int line, int column) {
        this.inicial = inicial;
        this.condicion = condicion;
        this.paso = paso;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFor(this);
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