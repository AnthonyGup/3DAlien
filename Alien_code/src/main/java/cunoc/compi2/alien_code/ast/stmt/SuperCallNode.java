package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class SuperCallNode extends Sentencia {
    public List<Node> argumentos;
    private final int line;
    private final int column;

    public SuperCallNode(List<Node> argumentos, int line, int column) {
        this.argumentos = argumentos;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!ctx.enConstructor()) {
            ctx.registrarError(getLine(), getColumn(),
                "super(...) solo puede usarse en constructores de una clase que hereda");
        }
        return Type.VOID;
    }
}
