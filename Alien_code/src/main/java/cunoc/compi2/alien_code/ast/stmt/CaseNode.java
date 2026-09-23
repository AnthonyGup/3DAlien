package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class CaseNode implements Node {
    public Node valor;
    public List<Node> sentencias;
    private final int line;
    private final int column;

    public CaseNode(Node valor, List<Node> sentencias, int line, int column) {
        this.valor = valor;
        this.sentencias = sentencias;
        this.line = line;
        this.column = column;
    }

    public boolean esSiempre() { return valor == null; }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitCase(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        if (!esSiempre()) {
            String valorText = valor.traducir(ctx);
            ctx.emitir("case", valorText, null, null);
        } else {
            ctx.emitir("case", "default", null, null);
        }
        for (Node sentencia : sentencias) {
            sentencia.traducir(ctx);
        }
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!esSiempre()) {
            ctx.evaluar(valor);
        }
        for (Node sentencia : sentencias) {
            ctx.evaluar(sentencia);
        }
        return Type.VOID;
    }
}
