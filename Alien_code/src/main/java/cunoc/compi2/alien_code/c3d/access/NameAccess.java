package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Collections;
import java.util.List;

public class NameAccess extends MemoryAccess {
    private final String nombre;
    private final boolean autoreferencia;

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

    @Override
    public void toCCode(StringBuilder sb) {
        if (autoreferencia) {
            sb.append("self->").append(nombre);
        } else {
            sb.append(nombre);
        }
    }

    @Override
    public List<String> referenceNames() {
        return autoreferencia ? Collections.emptyList() : Collections.singletonList(nombre);
    }
}