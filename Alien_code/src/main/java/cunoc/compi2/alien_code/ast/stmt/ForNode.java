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

public class ForNode extends Sentencia {
    public Node inicial;
    public Node condicion;
    public Node paso;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ForNode(Node inicial, Node condicion, Node paso, BlockNode cuerpo, int line, int column) {
        this.inicial = inicial;
        this.condicion = condicion;
        this.paso = paso;
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
        if (inicial != null) Operandos.ejecutar(ctx, inicial);
        LabelAccess start = Operandos.etiqueta(ctx);
        LabelAccess cont = Operandos.etiqueta(ctx);
        LabelAccess end = Operandos.etiqueta(ctx);
        ctx.agregar(new Etiqueta3D(start));
        if (condicion != null) {
            MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
            ctx.agregar(new Condicional3D(false, cond, end));
        }
        ctx.empujarCiclo(cont.getNombre(), end.getNombre());
        cuerpo.traducir(ctx);
        ctx.popCiclo();
        ctx.agregar(new Etiqueta3D(cont));
        if (paso != null) Operandos.ejecutar(ctx, paso);
        ctx.agregar(new Goto3D(start));
        ctx.agregar(new Etiqueta3D(end));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarAmbito();
        if (inicial != null) ctx.evaluar(inicial);

        if (condicion != null) {
            Type tipoCondicion = ctx.evaluar(condicion);
            if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
                ctx.registrarError(getLine(), getColumn(), "La condición de 'per' debe ser booleana, se dio " + tipoCondicion);
            }
        }
        if (paso != null) ctx.evaluar(paso);

        ctx.entrarCiclo();
        ctx.evaluar(cuerpo);
        ctx.salirCiclo();
        ctx.salirAmbito();
        return Type.VOID;
    }
}