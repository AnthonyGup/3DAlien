package cunoc.compi2.alien_code.ast;

public interface Node {
    <T> T accept(ASTVisitor<T> visitor);
    String traducir(CodigoContexto ctx);
    int getLine();
    int getColumn();
}
