package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;

public class VariableDeclNode implements Node {
    public String nombre;
    public Type tipo;
    public String tipoNombre;
    public Node inicial;
    private final int line;
    private final int column;

    public VariableDeclNode(String nombre, Type tipo, String tipoNombre, Node inicial, int line, int column) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.inicial = inicial;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariableDecl(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }
}