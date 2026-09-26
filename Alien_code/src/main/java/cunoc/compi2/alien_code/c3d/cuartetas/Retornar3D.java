package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Retornar3D extends Cuarteta {
    private final MemoryAccess valor;

    public Retornar3D(MemoryAccess valor) {
        this.valor = valor;
    }

    @Override
    public String operator() {
        return "return";
    }

    @Override
    public MemoryAccess getOperand1() {
        return valor;
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
        if (valor == null) {
            sb.append("return;");
            return;
        }
        sb.append("return ");
        sb.append(render(valor));
        sb.append(';');
    }
}