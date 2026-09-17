package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

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
        return null;
    }
}