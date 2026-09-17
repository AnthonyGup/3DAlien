package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class StructDeclNode implements Node {
    public String nombre;
    public List<VariableDeclNode> campos;
    private final int line;
    private final int column;

    public StructDeclNode(String nombre, List<VariableDeclNode> campos, int line, int column) {
        this.nombre = nombre;
        this.campos = campos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStructDecl(this);
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