package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.errors.ErrorListener;

public class SemanticAnalyzer implements ASTVisitor<Type> {
    private final SymbolTable symbolTable;
    private final ErrorListener errorListener;

    public SemanticAnalyzer(ErrorListener errorListener) {
        this.symbolTable = new SymbolTable();
        this.errorListener = errorListener;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public Type visitProgram(ProgramNode node) {
        return null;
    }
}
