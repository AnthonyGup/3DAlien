package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class WhileNode implements Node {
    public Node condicion;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public WhileNode(Node condicion, BlockNode cuerpo, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitWhile(this);
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