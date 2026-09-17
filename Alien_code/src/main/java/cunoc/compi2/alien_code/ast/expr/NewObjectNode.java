package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class NewObjectNode implements Node {
    public String nombreClase;
    public List<Node> argumentos;
    private final int line;
    private final int column;

    public NewObjectNode(String nombreClase, List<Node> argumentos, int line, int column) {
        this.nombreClase = nombreClase;
        this.argumentos = argumentos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitNewObject(this);
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
        Symbol clase = ctx.resolver(nombreClase);
        if (clase == null || clase.getKind() != Symbol.Kind.CLASE) {
            ctx.registrarError(getLine(), getColumn(),
                "'" + nombreClase + "' no es una clase conocida (¿falta un import .z?)");
            for (Node argumento : argumentos) {
                ctx.evaluar(argumento);
            }
            return null;
        }
        for (Node argumento : argumentos) {
            ctx.evaluar(argumento);
        }

        boolean algunoCoincide = clase.getMiembros() != null
            && clase.getMiembros().getTodos().stream()
                .anyMatch(s -> s.getKind() == Symbol.Kind.CONSTRUCTOR && s.getSize() == argumentos.size());
        if (!algunoCoincide) {
            ctx.registrarError(getLine(), getColumn(),
                "No existe un constructor de '" + nombreClase + "' con " + argumentos.size() + " argumento(s)");
            return null;
        }
        return Type.CLASS;
    }
}