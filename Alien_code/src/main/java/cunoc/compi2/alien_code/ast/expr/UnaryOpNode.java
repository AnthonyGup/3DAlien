package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class UnaryOpNode implements Node {
    public String operador;
    public Node operando;
    private final int line;
    private final int column;

    public UnaryOpNode(String operador, Node operando, int line, int column) {
        this.operador = operador;
        this.operando = operando;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryOp(this);
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
        Type tipo = ctx.evaluar(operando);
        if (operador.equals("non")) {
            if (tipo != null && tipo != Type.BOOL) {
                ctx.registrarError(getLine(), getColumn(), "'non' requiere un operando booleano, se dio " + tipo);
                return null;
            }
            return Type.BOOL;
        }
        if (operador.equals("-")) {
            if (tipo != null && !TypeCompat.esNumerico(tipo)) {
                ctx.registrarError(getLine(), getColumn(), "El signo '-' requiere un operando numérico, se dio " + tipo);
                return null;
            }
            return tipo;
        }
        return null;
    }
}