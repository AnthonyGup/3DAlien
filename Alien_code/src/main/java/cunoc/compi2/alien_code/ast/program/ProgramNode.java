package cunoc.compi2.alien_code.ast.program;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

import java.util.List;

public class ProgramNode implements Node {
    public List<Node> declarations;
    public String sourceLanguage;
    private int line;
    private int column;

    public ProgramNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitProgram(this);
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
        for (Node declaracion : declarations) {
            ctx.evaluar(declaracion);
        }
        return Type.VOID;
    }
}
