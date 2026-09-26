package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Goto3D extends Cuarteta {
    private final LabelAccess destino;

    public Goto3D(LabelAccess destino) {
        this.destino = destino;
    }

    @Override
    public String operator() {
        return "goto";
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
        return destino;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("goto ");
        sb.append(render(destino));
        sb.append(';');
    }
}