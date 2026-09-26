package cunoc.compi2.alien_code.ast;

import cunoc.compi2.alien_code.ir.CodigoContexto;

public abstract class Sentencia implements Node {
    public abstract void traducir(CodigoContexto ctx);
}