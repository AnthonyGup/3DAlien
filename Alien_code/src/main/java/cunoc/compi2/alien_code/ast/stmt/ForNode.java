package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;

public class ForNode implements Node {
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
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFor(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        gen.entrarAmbitoTraduccion();
        if (inicial != null) inicial.traducir(ctx);
        String start = ctx.nuevaEtiqueta();
        String cont = ctx.nuevaEtiqueta();
        String end = ctx.nuevaEtiqueta();
        ctx.emitir("label", null, null, start);
        if (condicion != null) {
            String cond = condicion.traducir(ctx);
            ctx.emitir("if_false", cond, null, end);
        }
        ctx.empujarCiclo(cont, end);
        cuerpo.traducir(ctx);
        ctx.popCiclo();
        ctx.emitir("label", null, null, cont);
        if (paso != null) paso.traducir(ctx);
        ctx.emitir("goto", null, null, start);
        ctx.emitir("label", null, null, end);
        gen.salirAmbitoTraduccion();
        return null;
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