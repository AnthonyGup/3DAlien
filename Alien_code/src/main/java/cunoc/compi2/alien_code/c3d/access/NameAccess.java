package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Collections;
import java.util.List;

public class NameAccess extends MemoryAccess {
    private final String nombre;
    private final boolean autoreferencia;
    private boolean global = false;

    public NameAccess(String nombre, Type tipo, String tipoNombre, boolean autoreferencia, int dimensiones) {
        super(tipo, tipoNombre, dimensiones);
        this.nombre = nombre;
        this.autoreferencia = autoreferencia;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isAutoreferencia() {
        return autoreferencia;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        if (autoreferencia) {
            sb.append("Heap[(int)(Stack[(int)(P + 2)]) + ").append(Math.max(0, getOffset())).append("]");
        } else if (getOffset() >= 0) {
            if (global) {
                sb.append("Stack[(int)").append(getOffset()).append("]");
            } else {
                sb.append("Stack[(int)(P + ").append(getOffset()).append(")]");
            }
        } else {
            sb.append(nombre);
        }
    }

    @Override
    public List<String> referenceNames() {
        return autoreferencia ? Collections.emptyList() : Collections.singletonList(nombre);
    }
}