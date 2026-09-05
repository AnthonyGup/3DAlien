package cunoc.compi2.alien_code.ast;

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
}
