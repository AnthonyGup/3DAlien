package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

import java.util.ArrayList;
import java.util.List;

public class Llamada3D extends Cuarteta {
    private final String funcion;
    private final List<MemoryAccess> argumentos;
    private final MemoryAccess destino;

    public Llamada3D(String funcion, List<MemoryAccess> argumentos, MemoryAccess destino) {
        this.funcion = funcion;
        this.argumentos = argumentos == null ? new ArrayList<>() : argumentos;
        this.destino = destino;
    }

    public String getFuncion() {
        return funcion;
    }

    public List<MemoryAccess> getArgumentos() {
        return argumentos;
    }

    @Override
    public String operator() {
        return "call";
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(funcion, null);
    }

    @Override
    public MemoryAccess getOperand2() {
        if (argumentos.isEmpty()) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }
            argumentos.get(i).toCCode(texto);
        }
        return new Literal3D(texto.toString(), null);
    }

    @Override
    public MemoryAccess getResult() {
        return destino;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        if (destino != null) {
            sb.append(render(destino));
            sb.append(" = ");
        }
        sb.append(funcion).append('(');
        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            argumentos.get(i).toCCode(sb);
        }
        sb.append(");");
    }
}