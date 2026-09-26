package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class ElseIfNode extends Sentencia {
    public Node condicion;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ElseIfNode(Node condicion, BlockNode cuerpo, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    public boolean esElse() {
        return condicion == null;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        if (esElse()) {
            if (cuerpo != null) {
                cuerpo.traducir(ctx);
            }
            return;
        }
        MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
        LabelAccess next = Operandos.etiqueta(ctx);
        ctx.agregar(new Condicional3D(false, cond, next));
        if (cuerpo != null) {
            cuerpo.traducir(ctx);
        }
        ctx.agregar(new Etiqueta3D(next));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!esElse()) {
            Type tipoCondicion = ctx.evaluar(condicion);
            if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
                ctx.registrarError(getLine(), getColumn(), "La condición debe ser booleana, se dio " + tipoCondicion);
            }
        }
        ctx.entrarAmbito();
        ctx.evaluar(cuerpo);
        ctx.salirAmbito();
        return Type.VOID;
    }
}