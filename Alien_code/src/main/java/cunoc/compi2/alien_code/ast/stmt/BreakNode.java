package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class BreakNode implements Node {
    private final int line;
    private final int column;

    public BreakNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBreak(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        ctx.emitir("goto", null, null, ctx.etiquetaSalidaActual());
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!ctx.enCiclo() && !ctx.enSwitch()) {
            ctx.registrarError(getLine(), getColumn(),
                "'romper'/'interrumpe' solo puede usarse dentro de un ciclo o un 'elegir'");
        }
        return Type.VOID;
    }
}