package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class ConditionalNode implements Node {
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
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConditional(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
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
