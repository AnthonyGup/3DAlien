package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public class DeclararArreglo3D extends Cuarteta {
    private final String nombre;
    private final String palabra;
    private final String dims;

    public DeclararArreglo3D(String nombre, String palabra, String dims) {
        this.nombre = nombre;
        this.palabra = palabra;
        this.dims = dims;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPalabra() {
        return palabra;
    }

    public String getDims() {
        return dims;
    }

    @Override
    public String operator() {
        return "array";
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(nombre, null);
    }

    @Override
    public MemoryAccess getOperand2() {
        return new Literal3D(dims == null ? "" : dims, null);
    }

    @Override
    public MemoryAccess getResult() {
        return new Literal3D(palabra, null);
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append("// array ").append(nombre);
        if (dims != null && !dims.isEmpty()) {
            sb.append(" (").append(dims).append(')');
        }
    }
}