package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class ConstructorDeclNode implements Node {
    public List<ParameterNode> parametros;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ConstructorDeclNode(List<ParameterNode> parametros, BlockNode cuerpo, int line, int column) {
        this.parametros = parametros;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstructorDecl(this);
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