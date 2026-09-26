package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Halt3D extends Cuarteta {
    @Override
    public String operator() {
        return "halt";
    }

    @Override
    public MemoryAccess getOperand1() {
        return null;
    }

    @Override
    public MemoryAccess getOperand2() {
        return null;
    }

    @Override
    public MemoryAccess getResult() {
        return null;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("exit(0);");
    }
}