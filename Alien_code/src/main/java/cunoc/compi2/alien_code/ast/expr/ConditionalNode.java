package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
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
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String cond = condicion.traducir(ctx);
        String t = ctx.nuevoTemporal();
        String falso = ctx.nuevaEtiqueta();
        ctx.emitir("if_false", cond, null, falso);
        String v = siVerdadero.traducir(ctx);
        ctx.emitir("=", v, null, t);
        String end = ctx.nuevaEtiqueta();
        ctx.emitir("goto", null, null, end);
        ctx.emitir("label", null, null, falso);
        String f = siFalso.traducir(ctx);
        ctx.emitir("=", f, null, t);
        ctx.emitir("label", null, null, end);
        gen.recordTemporalType(t, tipoResultado(v, f, gen));
        return t;
    }

    private Type tipoResultado(String v, String f, IntermediateCodeGenerator gen) {
        IntermediateCodeGenerator.ValueInfo iv = gen.describeValue(v);
        IntermediateCodeGenerator.ValueInfo iff = gen.describeValue(f);
        Type tipoV = iv != null ? iv.type : null;
        Type tipoF = iff != null ? iff.type : null;
        if (TypeCompat.esNumerico(tipoV) && TypeCompat.esNumerico(tipoF)) {
            return tipoV == Type.FLOAT || tipoF == Type.FLOAT ? Type.FLOAT : Type.INT;
        }
        return tipoV != null ? tipoV : tipoF;
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
