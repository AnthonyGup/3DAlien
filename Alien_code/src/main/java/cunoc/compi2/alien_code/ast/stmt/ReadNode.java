package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class ReadNode implements Node {
    public AccessNode destino;
    private final int line;
    private final int column;

    public ReadNode(AccessNode destino, int line, int column) {
        this.destino = destino;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitRead(this);
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
        if (destino != null) {
            Type tipoDestino = ctx.evaluar(destino);
            if (tipoDestino != null && tipoDestino != Type.STRING
                    && !TypeCompat.esNumerico(tipoDestino) && tipoDestino != Type.CHAR) {
                ctx.registrarError(getLine(), getColumn(),
                    "No se puede leer directamente hacia una variable de tipo " + tipoDestino);
            }
        }
        return Type.STRING;
    }
}