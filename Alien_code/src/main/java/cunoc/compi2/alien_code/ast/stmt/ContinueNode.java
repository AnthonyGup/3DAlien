package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class ContinueNode extends Sentencia {
    private final int line;
    private final int column;

    public ContinueNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        String cont = ctx.etiquetaContinuarActual();
        if (cont != null) {
            ctx.agregar(new Goto3D(new LabelAccess(cont)));
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!ctx.enCiclo()) {
            ctx.registrarError(getLine(), getColumn(), "'perge' solo puede usarse dentro de un ciclo");
        }
        return Type.VOID;
    }
}