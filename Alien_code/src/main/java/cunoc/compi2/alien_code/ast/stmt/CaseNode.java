package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class CaseNode extends Sentencia {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
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
