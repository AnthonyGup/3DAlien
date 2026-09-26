package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Collections;
import java.util.List;

public class Literal3D extends MemoryAccess {
    private final String texto;

    public Literal3D(String texto, Type tipo) {
        super(tipo);
        this.texto = texto;
    }

    public String getTexto() {
        return texto;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(texto);
    }

    @Override
    public List<String> referenceNames() {
        return Collections.emptyList();
    }
}