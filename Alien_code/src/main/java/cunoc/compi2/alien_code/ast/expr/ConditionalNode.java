package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class ConditionalNode extends Expresion {
    public Node condicion;
    public Node siVerdadero;
    public Node siFalso;
    private final int line;
    private final int column;

    public ConditionalNode(Node condicion, Node siVerdadero, Node siFalso, int line, int column) {
        this.condicion = condicion;
        this.siVerdadero = siVerdadero;
        this.siFalso = siFalso;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
        LabelAccess etiquetaFalso = Operandos.etiqueta(ctx);
        ctx.agregar(new Condicional3D(false, cond, etiquetaFalso));
        MemoryAccess v = ((Expresion) siVerdadero).traducir(ctx);
        MemoryAccess t = Operandos.temporal(ctx, Operandos.ctypeDe(ctx, v));
        ctx.agregar(new Asignacion3D(t, v));
        LabelAccess end = Operandos.etiqueta(ctx);
        ctx.agregar(new Goto3D(end));
        ctx.agregar(new Etiqueta3D(etiquetaFalso));
        MemoryAccess f = ((Expresion) siFalso).traducir(ctx);
        ctx.agregar(new Asignacion3D(t, f));
        ctx.agregar(new Etiqueta3D(end));
        return t;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipoCond = ctx.evaluar(condicion);
        if (tipoCond != Type.BOOL) {
            ctx.registrarError(getLine(), getColumn(),
                "La condición del operador ternario debe ser booleana, se dio " + tipoCond);
        }
        Type tipoV = ctx.evaluar(siVerdadero);
        Type tipoF = ctx.evaluar(siFalso);

        if (TypeCompat.esNumerico(tipoV) && TypeCompat.esNumerico(tipoF)) {
            return (tipoV == Type.FLOAT || tipoF == Type.FLOAT) ? Type.FLOAT : Type.INT;
        }
        if (!TypeCompat.sonComparables(tipoV, tipoF)) {
            ctx.registrarError(getLine(), getColumn(),
                "Las dos ramas del ternario deben ser del mismo tipo, se dio " + tipoV + " y " + tipoF);
        }
        return tipoV;
    }
}
