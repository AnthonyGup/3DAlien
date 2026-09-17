package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class BlockNode implements Node {
    public List<Node> sentencias;
    private final int line;
    private final int column;

    public BlockNode(int line, int column) {
        this.sentencias = new ArrayList<>();
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBlock(this);
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
        for (Node sentencia : sentencias) {
            ctx.evaluar(sentencia);
        }
        return Type.VOID;
    }
}