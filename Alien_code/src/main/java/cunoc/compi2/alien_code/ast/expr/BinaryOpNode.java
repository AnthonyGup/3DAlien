package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class BinaryOpNode implements Node {
    public String operador;
    public Node izquierda;
    public Node derecha;
    private final int line;
    private final int column;

    public BinaryOpNode(String operador, Node izquierda, Node derecha, int line, int column) {
        this.operador = operador;
        this.izquierda = izquierda;
        this.derecha = derecha;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryOp(this);
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
        Type izq = ctx.evaluar(izquierda);
        Type der = ctx.evaluar(derecha);

        switch (operador) {
            case "+": case "-": case "*": case "/": {
                Type resultado = TypeCompat.tipoAritmetico(izq, der);
                if (resultado == null && izq != null && der != null) {
                    ctx.registrarError(getLine(), getColumn(),
                        "Operación '" + operador + "' inválida entre " + izq + " y " + der);
                }
                if (!operador.equals("+") && resultado == Type.STRING) {
                    ctx.registrarError(getLine(), getColumn(),
                        "El operador '" + operador + "' no aplica a cadenas");
                    return null;
                }
                return resultado;
            }
            case "==": case "!=":
                if (!TypeCompat.sonComparables(izq, der)) {
                    ctx.registrarError(getLine(), getColumn(), "No se puede comparar " + izq + " con " + der);
                }
                return Type.BOOL;
            case "<": case ">":
                if (!TypeCompat.sonOrdenables(izq, der)) {
                    ctx.registrarError(getLine(), getColumn(),
                        "'" + operador + "' requiere tipos numéricos, se dio " + izq + " y " + der);
                }
                return Type.BOOL;
            case "&&": case "||":
                if ((izq != null && izq != Type.BOOL) || (der != null && der != Type.BOOL)) {
                    ctx.registrarError(getLine(), getColumn(), "'" + operador + "' requiere operandos booleanos");
                }
                return Type.BOOL;
            default:
                ctx.registrarError(getLine(), getColumn(), "Operador desconocido: " + operador);
                return null;
        }
    }
}