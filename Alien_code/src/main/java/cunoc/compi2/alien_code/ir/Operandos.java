package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.TiposC;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.access.TemporalAccess;
import cunoc.compi2.alien_code.semantic.Symbol;

public final class Operandos {
    private Operandos() {
    }

    public static TemporalAccess temporal(CodigoContexto ctx, String ctype) {
        int numero = ctx.nuevoIndiceTemporal();
        ctx.registrarTipoTemporal(numero, ctype);
        return new TemporalAccess(numero, TiposC.typeOfCType(ctype));
    }

    public static LabelAccess etiqueta(CodigoContexto ctx) {
        return new LabelAccess(ctx.nuevaEtiqueta());
    }

    public static NameAccess nombre(CodigoContexto ctx, String nombre) {
        return nombre(ctx, nombre, ctx.resolverSimbolo(nombre));
    }

    public static NameAccess nombre(CodigoContexto ctx, String nombre, Symbol simbolo) {
        boolean autoref = false;
        if (simbolo != null && simbolo.isField() && ctx.currentClassName() != null) {
            Symbol claseActual = ctx.resolverSimbolo(ctx.currentClassName());
            if (claseActual != null && claseActual.getMiembros() != null) {
                Symbol campoClase = claseActual.getMiembros().resolveLocal(nombre);
                if (campoClase != null && campoClase == simbolo) {
                    autoref = true;
                }
            }
        }
        return new NameAccess(nombre, tipoDe(simbolo), simbolo != null ? simbolo.getTipoNombre() : null,
                autoref, simbolo != null ? simbolo.getDimensiones() : 0);
    }

    public static Type tipoDe(Symbol simbolo) {
        if (simbolo == null) {
            return null;
        }
        return simbolo.getType();
    }

    public static Type tipoOperando(CodigoContexto ctx, MemoryAccess op) {
        if (op instanceof TemporalAccess temporal) {
            return TiposC.typeOfCType(ctx.tipoDeTemporal(temporal.getNumero()));
        }
        return op.getTipo();
    }

    public static String ctypeDe(CodigoContexto ctx, MemoryAccess op) {
        if (op instanceof TemporalAccess temporal) {
            String ctype = ctx.tipoDeTemporal(temporal.getNumero());
            if (ctype != null) {
                return ctype;
            }
        }
        return TiposC.cType(op.getTipo(), op.getTipoNombre());
    }

    public static String texto(MemoryAccess op) {
        StringBuilder sb = new StringBuilder();
        op.toCCode(sb);
        return sb.toString();
    }

    public static String tipoRetornoC(Type tipo, String tipoNombre) {
        if (tipo == null || tipo == Type.VOID) {
            return "void";
        }
        if (tipo == Type.ARRAY) {
            return "void*";
        }
        return TiposC.cType(tipo, tipoNombre);
    }

    public static Literal3D literalBraces(MemoryAccess destino, Literal3D raw) {
        if (destino.getDimensiones() > 0) {
            String elemento = TiposC.cType(destino.getTipo(), destino.getTipoNombre());
            return new Literal3D("(" + elemento + "[])" + raw.getTexto(), raw.getTipo());
        }
        Type tipo = destino.getTipo();
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            String nombre = destino.getTipoNombre() != null ? destino.getTipoNombre() : "X";
            String rawText = raw.getTexto();
            if ("NULL".equals(rawText)) {
                return new Literal3D("NULL", raw.getTipo());
            }
            return new Literal3D("&(struct " + nombre + ")" + rawText, raw.getTipo());
        }
        return raw;
    }

    public static void ejecutar(CodigoContexto ctx, Node nodo) {
        if (nodo instanceof Sentencia sentencia) {
            sentencia.traducir(ctx);
        } else if (nodo instanceof Expresion expresion) {
            expresion.traducir(ctx);
        }
    }
}