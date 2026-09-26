package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class FinFuncion3D extends Cuarteta {
    private final String nombre;

    public FinFuncion3D(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String operator() {
        return "func_end";
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(nombre == null ? "" : nombre, null);
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
        sb.append("// func_end");
        if (nombre != null && !nombre.isEmpty()) {
            sb.append(' ').append(nombre);
        }
    }
}