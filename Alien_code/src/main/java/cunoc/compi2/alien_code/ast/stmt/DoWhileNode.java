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

public class DoWhileNode extends Sentencia {
    public BlockNode cuerpo;
    public Node condicion;
    private final int line;
    private final int column;

    public DoWhileNode(BlockNode cuerpo, Node condicion, int line, int column) {
        this.cuerpo = cuerpo;
        this.condicion = condicion;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        LabelAccess start = Operandos.etiqueta(ctx);
        LabelAccess cont = Operandos.etiqueta(ctx);
        LabelAccess end = Operandos.etiqueta(ctx);
        ctx.agregar(new Etiqueta3D(start));
        ctx.empujarCiclo(cont.getNombre(), end.getNombre());
        cuerpo.traducir(ctx);
        ctx.popCiclo();
        ctx.agregar(new Etiqueta3D(cont));
        MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
        ctx.agregar(new Condicional3D(true, cond, start));
        ctx.agregar(new Etiqueta3D(end));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarCiclo();
        ctx.entrarAmbito();
        ctx.evaluar(cuerpo);
        ctx.salirAmbito();
        ctx.salirCiclo();

        Type tipoCondicion = ctx.evaluar(condicion);
        if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
            ctx.registrarError(getLine(), getColumn(), "La condición de 'dum' debe ser booleana, se dio " + tipoCondicion);
        }
        return Type.VOID;
    }
}