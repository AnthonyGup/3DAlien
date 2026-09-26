package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.ArrayList;
import java.util.List;

public class IndiceAccess extends MemoryAccess {
    private final MemoryAccess base;
    private final MemoryAccess indice;

    public IndiceAccess(MemoryAccess base, MemoryAccess indice, Type tipo, String tipoNombre, int dimensiones) {
        super(tipo, tipoNombre, dimensiones);
        this.base = base;
        this.indice = indice;
    }

    public MemoryAccess getBase() {
        return base;
    }

    public MemoryAccess getIndice() {
        return indice;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        base.toCCode(sb);
        sb.append('[');
        indice.toCCode(sb);
        sb.append(']');
    }

    @Override
    public List<String> referenceNames() {
        List<String> nombres = new ArrayList<>(base.referenceNames());
        nombres.addAll(indice.referenceNames());
        return nombres;
    }
}