package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class InicioFuncion3D extends Cuarteta {
    private final String nombre;
    private final String descripcion;
    private final String clase;
    private final String retorno;

    public InicioFuncion3D(String nombre, String descripcion, String clase, String retorno) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.clase = clase;
        this.retorno = retorno;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getClase() {
        return clase;
    }

    public String getRetorno() {
        return retorno;
    }

    @Override
    public String operator() {
        return "func";
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(nombre, null);
    }

    @Override
    public MemoryAccess getOperand2() {
        return new Literal3D(descripcion == null ? "" : descripcion, null);
    }

    @Override
    public MemoryAccess getResult() {
        return null;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("// func");
        if (nombre != null && !nombre.isEmpty()) {
            sb.append(' ').append(nombre);
        }
        if (descripcion != null && !descripcion.isEmpty()) {
            sb.append(" (").append(descripcion).append(')');
        }
    }
}