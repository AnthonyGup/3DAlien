package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class WhileNode extends Sentencia {
    public Node condicion;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public WhileNode(Node condicion, BlockNode cuerpo, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
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
        LabelAccess end = Operandos.etiqueta(ctx);
        ctx.agregar(new Etiqueta3D(start));
        MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
        ctx.agregar(new Condicional3D(false, cond, end));
        ctx.empujarCiclo(start.getNombre(), end.getNombre());
        cuerpo.traducir(ctx);
        ctx.popCiclo();
        ctx.agregar(new Goto3D(start));
        ctx.agregar(new Etiqueta3D(end));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipoCondicion = ctx.evaluar(condicion);
        if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
            ctx.registrarError(getLine(), getColumn(), "La condición de 'dum' debe ser booleana, se dio " + tipoCondicion);
        }
        ctx.entrarCiclo();
        ctx.entrarAmbito();
        ctx.evaluar(cuerpo);
        ctx.salirAmbito();
        ctx.salirCiclo();
        return Type.VOID;
    }
}