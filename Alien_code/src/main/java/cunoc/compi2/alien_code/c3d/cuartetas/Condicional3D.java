package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Condicional3D extends Cuarteta {
    private final boolean verdadero;
    private final MemoryAccess condicion;
    private final LabelAccess destino;

    public Condicional3D(boolean verdadero, MemoryAccess condicion, LabelAccess destino) {
        this.verdadero = verdadero;
        this.condicion = condicion;
        this.destino = destino;
    }

    @Override
    public String operator() {
        return verdadero ? "if_true" : "if_false";
    }

    @Override
    public MemoryAccess getOperand1() {
        return condicion;
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
        if (verdadero) {
            sb.append("if (");
            sb.append(render(condicion));
            sb.append(") goto ");
        } else {
            sb.append("if (!(");
            sb.append(render(condicion));
            sb.append(")) goto ");
        }
        sb.append(render(destino));
        sb.append(';');
    }
}