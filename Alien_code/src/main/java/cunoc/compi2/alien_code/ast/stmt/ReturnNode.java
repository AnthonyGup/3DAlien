package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class ReturnNode implements Node {
    public Node expresion;
    private final int line;
    private final int column;

    public ReturnNode(Node expresion, int line, int column) {
        this.expresion = expresion;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReturn(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        if (expresion != null) {
            String value = expresion.traducir(ctx);
            ctx.emitir("return", value, null, null);
        } else {
            ctx.emitir("return", null, null, null);
        }
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipoEsperado = ctx.tipoRetornoActual();
        Type tipoDado = expresion != null ? ctx.evaluar(expresion) : Type.VOID;

        if (tipoEsperado == null && expresion != null) {
            ctx.registrarError(getLine(), getColumn(), "Esta función no debe retornar ningún valor");
        } else if (tipoEsperado != null && !TypeCompat.esAsignable(tipoEsperado, tipoDado)) {
            ctx.registrarError(getLine(), getColumn(),
                "Se esperaba retornar " + tipoEsperado + " pero se dio " + tipoDado);
        }
        return tipoDado;
    }
}
