package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.NewArrayNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class VariableDeclNode implements Node {
    public String nombre;
    public Type tipo;
    public String tipoNombre;
    public Type tipoElemento;
    public int dimensiones = 1;
    public Node inicial;
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
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariableDecl(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            Symbol tipoSimbolo = ctx.resolver(tipoNombre);
            if (tipoSimbolo == null) {
                ctx.registrarError(getLine(), getColumn(),
                    "Tipo desconocido '" + tipoNombre + "' (¿falta un import?)");
            } else if (inicial instanceof StructLiteralNode) {
                ((StructLiteralNode) inicial).validarContra(tipoSimbolo, ctx);
            } else if (inicial != null) {
                ctx.evaluar(inicial);
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
            if (!TypeCompat.esAsignable(tipo, tipoInicial)) {
                ctx.registrarError(getLine(), getColumn(),
                    "No se puede asignar " + tipoInicial + " a variable de tipo " + tipo);
            }
        }

        Symbol simbolo = Symbol.variable(nombre, tipo, tipoElemento, tipoNombre,
                dimensiones, false, false, getLine(), getColumn());
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "La variable '" + nombre + "' ya fue declarada");
        }
        return tipo;
    }
}