package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class ReadNode extends Sentencia {
    public AccessNode destino;
    private final int line;
    private final int column;

    public ReadNode(AccessNode destino, int line, int column) {
        this.destino = destino;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        if (destino == null) {
            return;
        }
        MemoryAccess target = destino.toLvalue(ctx);
        Type tipo = target.getTipo();
        if (tipo == Type.STRING) {
            ctx.agregar(new Leer3D(target, "%s", false));
        } else if (tipo == Type.CHAR) {
            ctx.agregar(new Leer3D(target, " %c", true));
        } else if (tipo == Type.FLOAT) {
            ctx.agregar(new Leer3D(target, "%lf", true));
        } else {
            ctx.agregar(new Leer3D(target, "%d", true));
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (destino != null) {
            Type tipoDestino = ctx.evaluar(destino);
            if (tipoDestino != null && tipoDestino != Type.STRING
                    && !TypeCompat.esNumerico(tipoDestino) && tipoDestino != Type.CHAR) {
                ctx.registrarError(getLine(), getColumn(),
                    "No se puede leer directamente hacia una variable de tipo " + tipoDestino);
            }
        }
        return Type.STRING;
    }
}