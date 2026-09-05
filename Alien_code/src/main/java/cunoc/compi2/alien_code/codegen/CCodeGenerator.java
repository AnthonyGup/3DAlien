package cunoc.compi2.alien_code.codegen;

import cunoc.compi2.alien_code.c3d.C3DInstruction;

import java.util.List;

public class CCodeGenerator {
    private final StringBuilder code;

    public CCodeGenerator() {
        this.code = new StringBuilder();
    }

    public String generate(List<C3DInstruction> instructions) {
        code.setLength(0);
        code.append("#include <stdio.h>\n");
        code.append("#include <stdlib.h>\n\n");
        return code.toString();
    }

    public String getCode() {
        return code.toString();
    }
}
