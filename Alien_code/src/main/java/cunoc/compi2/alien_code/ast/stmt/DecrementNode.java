package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class DecrementNode extends Sentencia {
    public AccessNode objetivo;
    private final int line;
    private final int column;

    public DecrementNode(AccessNode objetivo, int line, int column) {
        this.objetivo = objetivo;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        MemoryAccess lvalue = objetivo.toLvalue(ctx);
        ctx.agregar(new Operacion3D("-", lvalue, new Literal3D("1", Type.INT), lvalue));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipo = ctx.evaluar(objetivo);
        if (tipo != null && !TypeCompat.esNumerico(tipo)) {
            ctx.registrarError(getLine(), getColumn(),
                "-- solo se puede usar sobre valores numéricos, no " + tipo);
        }
        return tipo;
    }
}