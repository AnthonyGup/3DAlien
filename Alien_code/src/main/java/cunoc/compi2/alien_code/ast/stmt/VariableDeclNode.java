package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.expr.StructLiteralNode;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

public class VariableDeclNode implements Node {
    public String nombre;
    public Type tipo;
    public String tipoNombre;
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
        } else if (inicial != null) {
            Type tipoInicial = ctx.evaluar(inicial);
            if (!TypeCompat.esAsignable(tipo, tipoInicial)) {
                ctx.registrarError(getLine(), getColumn(),
                    "No se puede asignar " + tipoInicial + " a variable de tipo " + tipo);
            }
        }

        Symbol simbolo = new Symbol(nombre, tipo, Symbol.Kind.VARIABLE, false, false, false, 0);
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            simbolo.setTipoNombre(tipoNombre);
        }
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "La variable '" + nombre + "' ya fue declarada");
        }
        return tipo;
    }
}