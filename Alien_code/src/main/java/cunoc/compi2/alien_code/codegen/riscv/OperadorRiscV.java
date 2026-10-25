package cunoc.compi2.alien_code.codegen.riscv;

public enum OperadorRiscV {
    SUMA("+"),
    RESTA("-"),
    MULTIPLICACION("*"),
    DIVISION("/"),
    RESIDUO("%"),
    MENOR("<"),
    MAYOR(">"),
    MENOR_IGUAL("<="),
    MAYOR_IGUAL(">="),
    IGUAL("=="),
    DISTINTO("!="),
    AND("&&"),
    OR("||"),
    NEGACION("-"),
    NO("!");

    private final String simbolo;

    OperadorRiscV(String simbolo) {
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }

    public static OperadorRiscV desde(String texto) {
        if (texto == null) {
            return null;
        }
        for (OperadorRiscV op : values()) {
            if (op.simbolo.equals(texto)) {
                return op;
            }
        }
        return null;
    }
}
