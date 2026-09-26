package cunoc.compi2.alien_code.ast;

import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public interface Node {
    int getLine();
    int getColumn();
    Type analizar(ContextoSemantico ctx);
}