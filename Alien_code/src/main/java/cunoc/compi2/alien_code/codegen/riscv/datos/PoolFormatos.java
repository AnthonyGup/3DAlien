package cunoc.compi2.alien_code.codegen.riscv.datos;

public final class PoolFormatos extends PoolBase {
    @Override
    protected void alRegistrar(String texto, int indice) {
        siguiente = indice + 1;
    }

    public String etiqueta(int indice) {
        return "lit_" + indice;
    }
}
