package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

import java.util.List;

public class IfNode implements Node {
    public Node condicion;
    public BlockNode cuerpo;
    public List<ElseIfNode> ramas;
    private final int line;
    private final int column;

    public IfNode(Node condicion, BlockNode cuerpo, List<ElseIfNode> ramas, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.ramas = ramas;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIf(this);
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