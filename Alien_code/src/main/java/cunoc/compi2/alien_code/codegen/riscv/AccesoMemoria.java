package cunoc.compi2.alien_code.codegen.riscv;

public final class AccesoMemoria {
    private AccesoMemoria() {}

    /**
     * Emite una instrucción de carga/guardado con offset posiblemente grande.
     * Si offset en [-2048, 2047]:  <instr> reg, offset(base)
     * Si no:                       li t6, offset; add t6, base, t6; <instr> reg, 0(t6)
     */
    public static void emitir(ContextoRiscV ctx, String instr, String reg, String base, int offset) {
        if (offset >= -2048 && offset <= 2047) {
            ctx.emitir("    " + instr + " " + reg + ", " + offset + "(" + base + ")");
        } else {
            ctx.emitir("    li t6, " + offset);
            ctx.emitir("    add t6, " + base + ", t6");
            ctx.emitir("    " + instr + " " + reg + ", 0(t6)");
        }
    }
}