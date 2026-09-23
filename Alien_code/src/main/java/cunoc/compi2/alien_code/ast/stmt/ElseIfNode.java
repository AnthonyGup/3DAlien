package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;

public class ElseIfNode implements Node {
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
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitElseIf(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        if (esElse()) {
            gen.entrarAmbitoTraduccion();
            if (cuerpo != null) {
                cuerpo.traducir(ctx);
            }
            gen.salirAmbitoTraduccion();
            return null;
        }
        String cond = condicion.traducir(ctx);
        String next = ctx.nuevaEtiqueta();
        ctx.emitir("if_false", cond, null, next);
        gen.entrarAmbitoTraduccion();
        if (cuerpo != null) {
            cuerpo.traducir(ctx);
        }
        gen.salirAmbitoTraduccion();
        ctx.emitir("label", null, null, next);
        return null;
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