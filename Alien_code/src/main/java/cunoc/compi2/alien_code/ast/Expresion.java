package cunoc.compi2.alien_code.ast;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.CodigoContexto;

public abstract class Expresion implements Node {
    public abstract MemoryAccess traducir(CodigoContexto ctx);
}