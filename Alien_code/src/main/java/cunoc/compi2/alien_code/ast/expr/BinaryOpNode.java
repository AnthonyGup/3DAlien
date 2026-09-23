package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
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
        if (operador.equals("&&") || operador.equals("||")) {
            return traducirCortocircuito(ctx);
        }
        String izq = izquierda.traducir(ctx);
        String der = derecha.traducir(ctx);
        String t = ctx.nuevoTemporal();
        ((IntermediateCodeGenerator) ctx).recordTemporalType(t, tipoResultado(izq, der, ctx));
        ctx.emitir(operador, izq, der, t);
        return t;
    }

    private Type tipoResultado(String izq, String der, CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        Type tipoIzq = null;
        Type tipoDer = null;
        IntermediateCodeGenerator.ValueInfo infoIzq = gen.describeValue(izq);
        if (infoIzq != null) tipoIzq = infoIzq.type;
        IntermediateCodeGenerator.ValueInfo infoDer = gen.describeValue(der);
        if (infoDer != null) tipoDer = infoDer.type;
        switch (operador) {
            case "==": case "!=": case "<": case ">": case "<=": case ">=":
                return Type.BOOL;
            default:
                return TypeCompat.tipoAritmetico(tipoIzq, tipoDer);
        }
    }

    private String traducirCortocircuito(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String izq = izquierda.traducir(ctx);
        String t = ctx.nuevoTemporal();
        gen.recordTemporalType(t, Type.BOOL);
        String constante = operador.equals("&&") ? "false" : "true";
        String corto = ctx.nuevaEtiqueta();
        ctx.emitir(operador.equals("&&") ? "if_false" : "if_true", izq, null, corto);
        String der = derecha.traducir(ctx);
        ctx.emitir("=", der, null, t);
        String end = ctx.nuevaEtiqueta();
        ctx.emitir("goto", null, null, end);
        ctx.emitir("label", null, null, corto);
        ctx.emitir("=", constante, null, t);
        ctx.emitir("label", null, null, end);
        return t;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type izq = ctx.evaluar(izquierda);
        Type der = ctx.evaluar(derecha);

        switch (operador) {
            case "+": case "-": case "*": case "/": case "%": {
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
            case "<": case ">": case "<=": case ">=":
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