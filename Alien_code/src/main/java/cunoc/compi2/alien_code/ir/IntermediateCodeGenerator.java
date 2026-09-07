package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.errors.ErrorListener;

import java.util.ArrayList;
import java.util.List;

public class IntermediateCodeGenerator implements ASTVisitor<String> {
    private final List<Cuarteta> cuartetas;
    private final ErrorListener errorListener;
    private int tempCounter;

    public IntermediateCodeGenerator(ErrorListener errorListener) {
        this.cuartetas = new ArrayList<>();
        this.errorListener = errorListener;
        this.tempCounter = 0;
    }

    public List<Cuarteta> getCuartetas() {
        return cuartetas;
    }

    public String newTemp() {
        return "t" + (tempCounter++);
    }

    @Override
    public String visitProgram(ProgramNode node) {
        return null;
    }
}