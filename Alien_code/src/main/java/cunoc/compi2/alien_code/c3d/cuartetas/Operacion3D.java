package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Operacion3D extends Cuarteta {
    private final String operador;
    private final MemoryAccess operando1;
    private final MemoryAccess operando2;
    private final MemoryAccess resultado;

    public Operacion3D(String operador, MemoryAccess operando1, MemoryAccess operando2, MemoryAccess resultado) {
        this.operador = operador;
        this.operando1 = operando1;
        this.operando2 = operando2;
        this.resultado = resultado;
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
        return resultado;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        if (operador.equals("conc") || operador.equals("strn") || operador.equals("strd")) {
            sb.append(render(resultado));
            sb.append(" = ").append(operador).append('(');
            sb.append(render(operando1));
            sb.append(", ");
            sb.append(render(operando2));
            sb.append(");");
            return;
        }
        if (resultado == null) {
            sb.append(render(operando1));
            sb.append(' ').append(operador).append(' ');
            sb.append(render(operando2));
            sb.append(';');
            return;
        }
        if (operando2 == null) {
            sb.append(render(resultado));
            sb.append(" = ").append(operador).append(' ');
            sb.append(render(operando1));
            sb.append(';');
            return;
        }
        sb.append(render(resultado));
        sb.append(" = ");
        sb.append(render(operando1));
        sb.append(' ').append(operador).append(' ');
        sb.append(render(operando2));
        sb.append(';');
    }
}