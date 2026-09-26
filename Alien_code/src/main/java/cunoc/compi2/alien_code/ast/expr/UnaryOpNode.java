package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class UnaryOpNode extends Expresion {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        MemoryAccess opd = ((Expresion) operando).traducir(ctx);
        String op = operador.equals("non") ? "!" : operador;
        String ctype = operador.equals("-") ? Operandos.ctypeDe(ctx, opd) : "int";
        MemoryAccess t = Operandos.temporal(ctx, ctype);
        ctx.agregar(new Operacion3D(op, opd, null, t));
        return t;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipo = ctx.evaluar(operando);
        if (operador.equals("non") || operador.equals("!")) {
            if (tipo != null && tipo != Type.BOOL) {
                ctx.registrarError(getLine(), getColumn(),
                    "La negación '" + operador + "' requiere un operando booleano, se dio " + tipo);
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