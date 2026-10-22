package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class ThisNode extends Expresion {
    private final int line;
    private final int column;

    public ThisNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        return new Literal3D("self", Type.CLASS);
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (!ctx.enRutina() || ctx.claseActual() == null) {
            ctx.registrarError(getLine(), getColumn(),
                "'this' solo puede usarse dentro de una clase");
            return null;
        }
        return Type.CLASS;
    }
}
