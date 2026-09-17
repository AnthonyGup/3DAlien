package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class ClassDeclNode implements Node {
    public String nombre;
    public List<VariableDeclNode> atributos;
    public List<ConstructorDeclNode> constructores;
    public List<MethodDeclNode> metodos;
    private final int line;
    private final int column;

    public ClassDeclNode(String nombre, List<VariableDeclNode> atributos, List<ConstructorDeclNode> constructores,
            List<MethodDeclNode> metodos, int line, int column) {
        this.nombre = nombre;
        this.atributos = atributos;
        this.constructores = constructores;
        this.metodos = metodos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitClassDecl(this);
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