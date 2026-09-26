package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Imprimir3D extends Cuarteta {
    private final String funcion;
    private final String formato;
    private final String args;

    public Imprimir3D(String funcion, String formato, String args) {
        this.funcion = funcion;
        this.formato = formato;
        this.args = args;
    }

    public String getFormato() {
        return formato;
    }

    public String getArgs() {
        return args;
    }

    @Override
    public String operator() {
        return funcion;
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(formato, null);
    }

    @Override
    public MemoryAccess getOperand2() {
        return args == null || args.isEmpty() ? null : new Literal3D(args, null);
    }

    @Override
    public MemoryAccess getResult() {
        return null;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(funcion).append('(').append(formato);
        if (args != null && !args.isEmpty()) {
            sb.append(", ").append(args);
        }
        sb.append(");");
    }
}