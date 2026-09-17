package cunoc.compi2.alien_code.ast;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import cunoc.compi2.alien_code.ir.CodigoContexto;

public interface Node {
    <T> T accept(ASTVisitor<T> visitor);
    String traducir(CodigoContexto ctx);
    int getLine();
    int getColumn();
    Type analizar(ContextoSemantico ctx);
}
