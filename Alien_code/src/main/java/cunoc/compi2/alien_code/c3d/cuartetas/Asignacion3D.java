package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Asignacion3D extends Cuarteta {
    private final MemoryAccess destino;
    private final MemoryAccess valor;

    public Asignacion3D(MemoryAccess destino, MemoryAccess valor) {
        this.destino = destino;
        this.valor = valor;
    }

    @Override
    public String operator() {
        return "=";
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
        return destino;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(render(destino));
        sb.append(" = ");
        sb.append(render(valor));
        sb.append(';');
    }
}