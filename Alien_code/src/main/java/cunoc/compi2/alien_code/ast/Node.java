package cunoc.compi2.alien_code.ast;

public interface Node {
    <T> T accept(ASTVisitor<T> visitor);
    int getLine();
    int getColumn();
}
