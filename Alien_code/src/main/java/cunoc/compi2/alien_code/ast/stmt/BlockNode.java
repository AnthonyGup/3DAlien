package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends Sentencia {
    public List<Node> sentencias;
    private final int line;
    private final int column;

    public BlockNode(int line, int column) {
        this.sentencias = new ArrayList<>();
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        for (Node sentencia : sentencias) {
            Operandos.ejecutar(ctx, sentencia);
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node sentencia : sentencias) {
            ctx.evaluar(sentencia);
        }
        return Type.VOID;
    }
}