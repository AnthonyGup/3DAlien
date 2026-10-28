package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.NewArrayNode;
import cunoc.compi2.alien_code.ast.expr.NewObjectNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class VariableDeclNode extends Sentencia {
    public String nombre;
    public Type tipo;
    public String tipoNombre;
    public Type tipoElemento;
    public int dimensiones = 1;
    public Node inicial;
    public cunoc.compi2.alien_code.ast.decl.Visibilidad visibilidad = cunoc.compi2.alien_code.ast.decl.Visibilidad.DEFAULT;
    private Symbol simboloResuelto;
    private final int line;
    private final int column;

    public VariableDeclNode(String nombre, Type tipo, String tipoNombre, Node inicial, int line, int column) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.inicial = inicial;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    private String nombreTipoLegible() {
        switch (tipo) {
            case INT: return "numerus";
            case FLOAT: return "decimalis";
            case STRING: return "textum";
            case CHAR: return "littera";
            case BOOL: return "bool";
            default: return tipo != null ? tipo.toString().toLowerCase() : "?";
        }
    }


    @Override
    public void traducir(CodigoContexto ctx) {
        if (inicial == null) {
            return;
        }
        MemoryAccess value = ((Expresion) inicial).traducir(ctx);
        MemoryAccess target = simboloResuelto != null
                ? Operandos.nombre(ctx, nombre, simboloResuelto)
                : Operandos.nombre(ctx, nombre, ctx.resolverSimbolo(nombre));
        if (value instanceof Literal3D literal) {
            value = Operandos.literalBraces(target, literal);
        }
        ctx.agregar(new Asignacion3D(target, value));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            Symbol tipoSimbolo = ctx.resolver(tipoNombre);
            if (tipoSimbolo == null) {
                tipoSimbolo = cunoc.compi2.alien_code.semantic.POO.buscarClase(ctx, tipoNombre);
            }
            if (tipoSimbolo == null) {
                ctx.registrarError(getLine(), getColumn(),
                    "La clase '" + tipoNombre + "' no existe");
            } else if (inicial instanceof StructLiteralNode) {
                ((StructLiteralNode) inicial).validarContra(tipoSimbolo, ctx);
            } else if (inicial instanceof NewObjectNode nuevo) {
                Type tipoInicial = ctx.evaluar(nuevo);
                if (tipoInicial != null && tipo == Type.CLASS
                        && !cunoc.compi2.alien_code.semantic.POO.esSubtipo(
                            ctx, nuevo.nombreClase, tipoNombre)) {
                    ctx.registrarError(getLine(), getColumn(),
                        "'" + nuevo.nombreClase + "' no es '" + tipoNombre
                        + "' ni hereda de ella");
                }
            } else if (inicial != null) {
                Type tipoInicial = ctx.evaluar(inicial);
                if (tipoInicial == Type.CLASS || tipoInicial == Type.STRUCT) {
                    Symbol fuente = inicial instanceof AccessNode acceso
                        ? AccessNode.resolverSimbolo(ctx, acceso) : null;
                    String origen = fuente != null ? fuente.getTipoNombre() : null;
                    if (origen != null && tipo == Type.CLASS
                            && !cunoc.compi2.alien_code.semantic.POO.esSubtipo(ctx, origen, tipoNombre)) {
                        ctx.registrarError(getLine(), getColumn(),
                            "No se puede asignar '" + origen + "' a '" + tipoNombre + "'");
                    }
                }
            }
        } else if (tipo == Type.ARRAY) {
            if (inicial instanceof NewArrayNode nuevo) {
                ctx.evaluar(nuevo);
                if (tipoElemento != null && nuevo.tipoElemento != null
                        && !TypeCompat.esAsignable(tipoElemento, nuevo.tipoElemento)) {
                    ctx.registrarError(nuevo.getLine(), nuevo.getColumn(),
                        "No se puede asignar arreglo de " + nuevo.tipoElemento + " a arreglo de "
                        + tipoElemento);
                }
            } else if (inicial instanceof StructLiteralNode literal) {
                for (Node valor : literal.valores) {
                    Type tipoValor = ctx.evaluar(valor);
                    if (tipoElemento != null && !TypeCompat.esAsignable(tipoElemento, tipoValor)) {
                        ctx.registrarError(valor.getLine(), valor.getColumn(),
                            "Elemento de tipo " + tipoValor + " no es compatible con arreglo de " + tipoElemento);
                    }
                }
            } else if (inicial != null) {
                Type tipoInicial = ctx.evaluar(inicial);
                Symbol fuente = null;
                if (tipoInicial != Type.ARRAY && inicial instanceof AccessNode acceso) {
                    Symbol candidato = AccessNode.resolverSimbolo(ctx, acceso);
                    if (candidato != null && candidato.isArray()) {
                        fuente = candidato;
                    }
                }
                if (tipoInicial != Type.ARRAY && fuente == null) {
                    ctx.registrarError(getLine(), getColumn(),
                        "No se puede asignar " + tipoInicial + " a variable de tipo arreglo");
                } else if (tipoElemento != null && fuente != null
                        && !TypeCompat.esAsignable(tipoElemento, fuente.getType())) {
                    ctx.registrarError(getLine(), getColumn(),
                        "No se puede asignar arreglo de " + fuente.getType() + " a arreglo de "
                        + tipoElemento);
                }
            }
        } else if (inicial != null) {
            Type tipoInicial = ctx.evaluar(inicial);
            if (tipoInicial == Type.NULL && tipo != Type.CLASS && tipo != Type.STRUCT
                    && tipo != Type.STRING) {
                ctx.registrarError(getLine(), getColumn(),
                    "null no puede asignarse a '" + nombreTipoLegible() + "'");
            } else if (!TypeCompat.esAsignable(tipo, tipoInicial)) {
                ctx.registrarError(getLine(), getColumn(),
                    "No se puede asignar " + tipoInicial + " a variable de tipo " + tipo);
            }
        }

        Symbol simbolo = Symbol.variable(nombre, tipo, tipoElemento, tipoNombre,
                dimensiones, false, false, getLine(), getColumn());
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "La variable '" + nombre + "' ya fue declarada");
        }
        simboloResuelto = simbolo;
        return tipo;
    }
}