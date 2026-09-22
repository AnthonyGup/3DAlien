package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.expr.NewArrayNode;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class AssignmentNode implements Node {
    public AccessNode destino;
    public Node valor;
    private final int line;
    private final int column;

    public AssignmentNode(AccessNode destino, Node valor, int line, int column) {
        this.destino = destino;
        this.valor = valor;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignment(this);
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
        if (valor instanceof StructLiteralNode literal) {
            Symbol arreglo = resolverArregloDestino(ctx);
            if (arreglo != null) {
                for (Node elemento : literal.valores) {
                    Type tipoElemento = ctx.evaluar(elemento);
                    if (!TypeCompat.esAsignable(arreglo.getType(), tipoElemento)) {
                        ctx.registrarError(elemento.getLine(), elemento.getColumn(),
                            "Elemento de tipo " + tipoElemento + " no es compatible con arreglo de "
                            + arreglo.getType());
                    }
                }
                if (arreglo.getSize() > 0 && literal.valores.size() != arreglo.getSize()) {
                    ctx.registrarError(getLine(), getColumn(),
                        "El arreglo declara tamaño " + arreglo.getSize() + " pero el inicializador tiene "
                        + literal.valores.size() + " elementos");
                }
                return arreglo.getType();
            }
        }
        if (valor instanceof NewArrayNode nuevo) {
            Symbol arreglo = resolverArregloDestino(ctx);
            if (arreglo != null) {
                ctx.evaluar(nuevo);
                if (nuevo.tipoElemento != null
                        && !TypeCompat.esAsignable(arreglo.getType(), nuevo.tipoElemento)) {
                    ctx.registrarError(nuevo.getLine(), nuevo.getColumn(),
                        "No se puede asignar arreglo de " + nuevo.tipoElemento + " a arreglo de "
                        + arreglo.getType());
                }
                if (arreglo.getDimensiones() > 0
                        && nuevo.dimensiones.size() != arreglo.getDimensiones()) {
                    ctx.registrarError(nuevo.getLine(), nuevo.getColumn(),
                        "El arreglo espera " + arreglo.getDimensiones()
                        + " dimension(es) pero se dieron " + nuevo.dimensiones.size());
                }
                return arreglo.getType();
            }
        }
        Type tipoDestino = ctx.evaluar(destino);
        Type tipoValor = ctx.evaluar(valor);
        if (!TypeCompat.esAsignable(tipoDestino, tipoValor)) {
            ctx.registrarError(getLine(), getColumn(),
                "No se puede asignar " + tipoValor + " a " + tipoDestino);
        }
        return tipoDestino;
    }

    private Symbol resolverArregloDestino(ContextoSemantico ctx) {
        if (destino.sufijos.isEmpty()) {
            Symbol simbolo = ctx.resolver(destino.nombre);
            return simbolo != null && simbolo.isArray() ? simbolo : null;
        }
        if (destino.sufijos.size() == 1
                && destino.sufijos.get(0).tipo == AccessNode.Sufijo.Tipo.CAMPO) {
            Symbol base = ctx.resolver(destino.nombre);
            Symbol tipoBase = base != null && base.getTipoNombre() != null
                    ? ctx.resolver(base.getTipoNombre()) : null;
            if (tipoBase != null && tipoBase.getMiembros() != null) {
                Symbol miembro = tipoBase.getMiembros()
                        .resolve(destino.sufijos.get(0).nombreCampo);
                if (miembro != null && miembro.isArray()) {
                    return miembro;
                }
            }
        }
        return null;
    }
}
