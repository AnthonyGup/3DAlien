package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;

import java.util.List;

public class IfNode implements Node {
    public Node condicion;
    public BlockNode cuerpo;
    public List<ElseIfNode> ramas;
    private final int line;
    private final int column;

    public IfNode(Node condicion, BlockNode cuerpo, List<ElseIfNode> ramas, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.ramas = ramas;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIf(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String cond = condicion.traducir(ctx);
        String end = ctx.nuevaEtiqueta();
        String elseLabel = ramas.isEmpty() ? end : ctx.nuevaEtiqueta();
        ctx.emitir("if_false", cond, null, elseLabel);

        gen.entrarAmbitoTraduccion();
        if (cuerpo != null) {
            cuerpo.traducir(ctx);
        }
        gen.salirAmbitoTraduccion();

        if (ramas.isEmpty()) {
            ctx.emitir("label", null, null, end);
            return null;
        }

        ctx.emitir("goto", null, null, end);
        ctx.emitir("label", null, null, elseLabel);
        for (int i = 0; i < ramas.size(); i++) {
            ElseIfNode rama = ramas.get(i);
            if (i == ramas.size() - 1 && rama.esElse()) {
                gen.entrarAmbitoTraduccion();
                rama.cuerpo.traducir(ctx);
                gen.salirAmbitoTraduccion();
            } else {
                String next = ctx.nuevaEtiqueta();
                String condRama = rama.condicion.traducir(ctx);
                ctx.emitir("if_false", condRama, null, next);
                gen.entrarAmbitoTraduccion();
                rama.cuerpo.traducir(ctx);
                gen.salirAmbitoTraduccion();
                ctx.emitir("goto", null, null, end);
                ctx.emitir("label", null, null, next);
            }
        }
        ctx.emitir("label", null, null, end);
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipoCondicion = ctx.evaluar(condicion);
        if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
            ctx.registrarError(getLine(), getColumn(), "La condición debe ser booleana, se dio " + tipoCondicion);
        }
        ctx.entrarAmbito();
        ctx.evaluar(cuerpo);
        ctx.salirAmbito();

        for (ElseIfNode rama : ramas) {
            ctx.evaluar(rama);
        }
        return Type.VOID;
    }
}