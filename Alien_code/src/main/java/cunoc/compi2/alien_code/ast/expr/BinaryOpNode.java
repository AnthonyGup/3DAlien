package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.TiposC;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.List;

public class BinaryOpNode extends Expresion {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        if (operador.equals("&&") || operador.equals("||")) {
            return traducirCortocircuito(ctx);
        }
        MemoryAccess izq = ((Expresion) izquierda).traducir(ctx);
        MemoryAccess der = ((Expresion) derecha).traducir(ctx);
        Type tipoIzq = Operandos.tipoOperando(ctx, izq);
        Type tipoDer = Operandos.tipoOperando(ctx, der);
        if (operador.equals("+") && (tipoIzq == Type.STRING || tipoDer == Type.STRING)) {
            MemoryAccess t = Operandos.temporal(ctx, "char*");
            ctx.agregar(new Llamada3D("conc", List.of(izq, der), t));
            return t;
        }
        String ctype = TiposC.binaryResultType(tipoIzq, tipoDer, operador);
        MemoryAccess t = Operandos.temporal(ctx, ctype);
        ctx.agregar(new Operacion3D(operador, izq, der, t));
        return t;
    }

    private MemoryAccess traducirCortocircuito(CodigoContexto ctx) {
        MemoryAccess izq = ((Expresion) izquierda).traducir(ctx);
        MemoryAccess t = Operandos.temporal(ctx, "int");
        boolean and = operador.equals("&&");
        LabelAccess corto = Operandos.etiqueta(ctx);
        ctx.agregar(new Condicional3D(!and, izq, corto));
        MemoryAccess der = ((Expresion) derecha).traducir(ctx);
        ctx.agregar(new Asignacion3D(t, der));
        LabelAccess end = Operandos.etiqueta(ctx);
        ctx.agregar(new Goto3D(end));
        ctx.agregar(new Etiqueta3D(corto));
        ctx.agregar(new Asignacion3D(t, new Literal3D(and ? "0" : "1", Type.BOOL)));
        ctx.agregar(new Etiqueta3D(end));
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