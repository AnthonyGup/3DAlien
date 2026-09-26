package cunoc.compi2.alien_code.c3d.access;

import cunoc.compi2.alien_code.ast.Type;

import java.util.List;

public class CampoAccess extends MemoryAccess {
    private final MemoryAccess base;
    private final String campo;

    public CampoAccess(MemoryAccess base, String campo, Type tipo, String tipoNombre, int dimensiones) {
        super(tipo, tipoNombre, dimensiones);
        this.base = base;
        this.campo = campo;
    }

    public MemoryAccess getBase() {
        return base;
    }

    public String getCampo() {
        return campo;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        base.toCCode(sb);
        sb.append("->").append(campo);
    }

    @Override
    public List<String> referenceNames() {
        return base.referenceNames();
    }
}