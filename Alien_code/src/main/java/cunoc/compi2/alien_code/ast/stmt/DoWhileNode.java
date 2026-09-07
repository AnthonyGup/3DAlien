package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class DoWhileNode implements Node {
    public BlockNode cuerpo;
    public Node condicion;
    private final int line;
    private final int column;

    public DoWhileNode(BlockNode cuerpo, Node condicion, int line, int column) {
        this.cuerpo = cuerpo;
        this.condicion = condicion;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDoWhile(this);
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