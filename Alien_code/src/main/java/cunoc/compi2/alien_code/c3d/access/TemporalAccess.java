package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.Collections;
import java.util.List;

public class TemporalAccess extends MemoryAccess {
    private final int numero;

    public TemporalAccess(int numero, Type tipo) {
        super(tipo);
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append('t').append(numero);
    }

    @Override
    public List<String> referenceNames() {
        return Collections.singletonList("t" + numero);
    }
}