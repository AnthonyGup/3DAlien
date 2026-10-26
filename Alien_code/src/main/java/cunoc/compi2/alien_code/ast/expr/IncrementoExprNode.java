package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.TiposC;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

/**
 * Incremento/decremento POSTFIJO usado como expresion (k++ / k--).
 * Devuelve el valor ANTERIOR y aplica el +/- 1 sobre el destino
 * (misma emision que IncrementNode/DecrementNode, mas el temporal).
 * El prefijo (++k) NO esta soportado.
 */
public class IncrementoExprNode extends Expresion {

    public final AccessNode destino;
    public final boolean esIncremento;
    private final int line;
    private final int column;

    public IncrementoExprNode(AccessNode destino, boolean esIncremento, int line, int column) {
        this.destino = destino;
        this.esIncremento = esIncremento;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        MemoryAccess lvalue = destino.toLvalue(ctx);
        String operador = esIncremento ? "+" : "-";
        Type tipo = lvalue.getTipo();
        MemoryAccess viejo = Operandos.temporal(ctx, TiposC.cType(tipo, lvalue.getTipoNombre()));
        ctx.agregar(new Asignacion3D(viejo, lvalue));
        ctx.agregar(new Operacion3D(operador, lvalue,
                new Literal3D("1", Type.INT), lvalue));
        return viejo;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!destino.sufijos.isEmpty()
                && destino.sufijos.get(destino.sufijos.size() - 1).tipo
                        == AccessNode.Sufijo.Tipo.LLAMADA) {
            ctx.registrarError(getLine(), getColumn(),
                "No se puede aplicar '" + (esIncremento ? "++" : "--")
                + "' al resultado de una llamada");
            return null;
        }
        Type tipo = ctx.evaluar(destino);
        if (tipo != null && !TypeCompat.esNumerico(tipo)) {
            ctx.registrarError(getLine(), getColumn(),
                "El operando de '" + (esIncremento ? "++" : "--")
                + "' debe ser numérico, no " + tipo);
            return null;
        }
        return tipo;
    }
}
