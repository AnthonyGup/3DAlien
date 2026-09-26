package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Impresion;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.ArrayList;
import java.util.List;

public class PrintNode extends Sentencia {
    public List<Node> expresiones;
    private final int line;
    private final int column;

    public PrintNode(List<Node> expresiones, int line, int column) {
        this.expresiones = expresiones;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        List<MemoryAccess> argumentos = new ArrayList<>();
        for (Node expresion : expresiones) {
            argumentos.add(((Expresion) expresion).traducir(ctx));
        }
        ctx.agregar(Impresion.print(ctx, argumentos));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node expresion : expresiones) {
            ctx.evaluar(expresion);
        }
        return Type.VOID;
    }
}