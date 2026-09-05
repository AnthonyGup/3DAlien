package cunoc.compi2.alien_code.c3d;

import cunoc.compi2.alien_code.ir.Cuarteta;

import java.util.ArrayList;
import java.util.List;

public class C3DGenerator {
    private final List<C3DInstruction> instructions;
    private int labelCounter;

    public C3DGenerator() {
        this.instructions = new ArrayList<>();
        this.labelCounter = 0;
    }

    public List<C3DInstruction> getInstructions() {
        return instructions;
    }

    public String newLabel() {
        return "L" + (labelCounter++);
    }

    public void generate(List<Cuarteta> cuartetas) {
    }
}
