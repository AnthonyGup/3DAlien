package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Marcador3D extends Cuarteta {
    private final String operador;
    private final MemoryAccess operando1;
    private final MemoryAccess operando2;

    public Marcador3D(String operador, MemoryAccess operando1, MemoryAccess operando2) {
        this.operador = operador;
        this.operando1 = operando1;
        this.operando2 = operando2;
    }

    @Override
    public String operator() {
        return operador;
    }

    @Override
    public MemoryAccess getOperand1() {
        return operando1;
    }

    @Override
    public MemoryAccess getOperand2() {
        return operando2;
    }

    @Override
    public MemoryAccess getResult() {
        return null;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("// ").append(operador);
        if (operando1 != null) {
            sb.append(' ').append(render(operando1));
        }
        if (operando2 != null) {
            sb.append(" (").append(render(operando2)).append(')');
        }
    }
}