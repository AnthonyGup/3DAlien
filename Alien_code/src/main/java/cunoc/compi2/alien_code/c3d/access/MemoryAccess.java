package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.CodeTransformable;

import java.util.List;

public abstract class MemoryAccess implements CodeTransformable {
    private final Type tipo;
    private final String tipoNombre;
    private final int dimensiones;
    private int offset = -1;

    protected MemoryAccess(Type tipo) {
        this(tipo, null, 0);
    }

    protected MemoryAccess(Type tipo, String tipoNombre, int dimensiones) {
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.dimensiones = dimensiones;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public Type getTipo() {
        return tipo;
    }

    public String getTipoNombre() {
        return tipoNombre;
    }

    public int getDimensiones() {
        return dimensiones;
    }

    public abstract List<String> referenceNames();
}