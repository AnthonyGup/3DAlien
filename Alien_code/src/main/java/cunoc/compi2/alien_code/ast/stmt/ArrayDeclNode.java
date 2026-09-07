package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;

import java.util.List;

public class ArrayDeclNode implements Node {
    public String nombre;
    public int tamano;
    public Type tipo;
    public String tipoNombre;
    public List<Node> iniciales;
    private final int line;
    private final int column;

    public ArrayDeclNode(String nombre, int tamano, Type tipo, String tipoNombre, List<Node> iniciales, int line, int column) {
        this.nombre = nombre;
        this.tamano = tamano;
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.iniciales = iniciales;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArrayDecl(this);
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