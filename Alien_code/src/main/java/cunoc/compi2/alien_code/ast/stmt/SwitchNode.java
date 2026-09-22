package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class SwitchNode implements Node {
    public Node expresion;
    public List<CaseNode> casos;
    private final int line;
    private final int column;

    public SwitchNode(Node expresion, List<CaseNode> casos, int line, int column) {
        this.expresion = expresion;
        this.casos = casos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSwitch(this);
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
        ctx.evaluar(expresion);
        ctx.entrarSwitch();
        for (CaseNode caso : casos) {
            ctx.evaluar(caso);
        }
        ctx.salirSwitch();
        return Type.VOID;
    }
}
