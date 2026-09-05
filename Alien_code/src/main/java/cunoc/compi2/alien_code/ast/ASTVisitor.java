package cunoc.compi2.alien_code.ast;

public interface ASTVisitor<T> {
    T visitProgram(ProgramNode node);
}
