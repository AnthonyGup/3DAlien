package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class BreakNode extends Sentencia {
    private final int line;
    private final int column;

    public BreakNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        String salida = ctx.etiquetaSalidaActual();
        if (salida != null) {
            ctx.agregar(new Goto3D(new LabelAccess(salida)));
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!ctx.enCiclo() && !ctx.enSwitch()) {
            ctx.registrarError(getLine(), getColumn(),
                "'romper'/'interrumpe' solo puede usarse dentro de un ciclo o un 'elegir'");
        }
        return Type.VOID;
    }
}