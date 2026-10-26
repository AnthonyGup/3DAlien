package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ast.Type;

public enum TipoRV {
    ENTERO(4, 4, "lw", "sw", "add", "sub", "mul", "div", "rem"),
    REF(4, 4, "lw", "sw", "add", "sub", "mul", "div", "rem"),
    CHAR(1, 1, "lbu", "sb", null, null, null, null, null),
    BOOL(1, 1, "lbu", "sb", null, null, null, null, null),
    DOBLE(8, 8, "fld", "fsd", "fadd.d", "fsub.d", "fmul.d", "fdiv.d", null);

    public final int tamano;
    public final int alineacion;
    public final String carga;
    public final String guarda;
    public final String opAdd;
    public final String opSub;
    public final String opMul;
    public final String opDiv;
    public final String opRem;

    TipoRV(int tamano, int alineacion, String carga, String guarda,
            String opAdd, String opSub, String opMul, String opDiv, String opRem) {
        this.tamano = tamano;
        this.alineacion = alineacion;
        this.carga = carga;
        this.guarda = guarda;
        this.opAdd = opAdd;
        this.opSub = opSub;
        this.opMul = opMul;
        this.opDiv = opDiv;
        this.opRem = opRem;
    }

    public boolean esEntero() {
        return this == ENTERO || this == REF || this == CHAR || this == BOOL;
    }

    public boolean esDouble() {
        return this == DOBLE;
    }

    public static TipoRV desde(Type t) {
        if (t == null) return ENTERO;
        return switch (t) {
            case INT -> ENTERO;
            case FLOAT -> DOBLE;
            case STRING -> REF;
            case BOOL -> BOOL;
            case CHAR -> CHAR;
            case STRUCT -> REF;
            case CLASS -> REF;
            case ARRAY -> REF;
            case NULL -> REF;
            case VOID -> ENTERO;
            default -> ENTERO;
        };
    }

    public static TipoRV desdeString(String ctype) {
        if (ctype == null) return ENTERO;
        String c = ctype.toLowerCase().trim();
        if (c.contains("[]") || c.endsWith("*") || c.contains("struct")) return REF;
        if (c.contains("double") || c.contains("float")) return DOBLE;
        if (c.contains("bool")) return BOOL;
        if (c.contains("char")) return CHAR;
        if (c.contains("int") || c.contains("void")) return ENTERO;
        return REF;
    }

    public String cargaSlot() {
        return (this == DOBLE) ? "fld" : "lw";
    }

    public String guardaSlot() {
        return (this == DOBLE) ? "fsd" : "sw";
    }
}