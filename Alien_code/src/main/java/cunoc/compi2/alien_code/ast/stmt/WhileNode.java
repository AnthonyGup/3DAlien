package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;

public class WhileNode implements Node {
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
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitWhile(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String start = ctx.nuevaEtiqueta();
        String end = ctx.nuevaEtiqueta();
        ctx.emitir("label", null, null, start);
        String cond = condicion.traducir(ctx);
        ctx.emitir("if_false", cond, null, end);
        ctx.empujarCiclo(start, end);
        gen.entrarAmbitoTraduccion();
        cuerpo.traducir(ctx);
        gen.salirAmbitoTraduccion();
        ctx.popCiclo();
        ctx.emitir("goto", null, null, start);
        ctx.emitir("label", null, null, end);
        return null;
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