package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Collections;
import java.util.List;

public class LabelAccess extends MemoryAccess {
    private final String nombre;

    public LabelAccess(String nombre) {
        super(null);
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(nombre);
    }

    @Override
    public List<String> referenceNames() {
        return Collections.emptyList();
    }
}