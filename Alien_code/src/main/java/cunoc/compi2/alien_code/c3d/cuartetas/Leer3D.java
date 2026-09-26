package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class Leer3D extends Cuarteta {
    private final MemoryAccess objetivo;
    private final String formato;
    private final boolean conAmpersand;

    public Leer3D(MemoryAccess objetivo, String formato, boolean conAmpersand) {
        this.objetivo = objetivo;
        this.formato = formato;
        this.conAmpersand = conAmpersand;
    }

    public String getFormato() {
        return formato;
    }

    public boolean isConAmpersand() {
        return conAmpersand;
    }

    @Override
    public String operator() {
        return "scanf";
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
        return objetivo;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("scanf(\"").append(formato).append("\", ");
        if (conAmpersand) {
            sb.append('&');
        }
        sb.append(render(objetivo));
        sb.append(");");
    }
}