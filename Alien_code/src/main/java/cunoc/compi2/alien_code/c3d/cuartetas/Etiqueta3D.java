package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Etiqueta3D extends Cuarteta {
    private final LabelAccess nombre;

    public Etiqueta3D(LabelAccess nombre) {
        this.nombre = nombre;
    }

    @Override
    public String operator() {
        return "label";
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
        return nombre;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(render(nombre));
        sb.append(": ;");
    }
}