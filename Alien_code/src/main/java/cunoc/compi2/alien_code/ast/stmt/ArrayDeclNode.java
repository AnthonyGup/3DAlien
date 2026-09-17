package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.List;

public class ArrayDeclNode implements Node {
    public String nombre;
    public int tamano;
    public Type tipo;
    public String tipoNombre;
    public List<Node> iniciales;
    private final int line;
    private final int column;

    public ArrayDeclNode(String nombre, int tamano, Type tipo, String tipoNombre, List<Node> iniciales, int line, int column) {
        this.nombre = nombre;
        this.tamano = tamano;
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.iniciales = iniciales;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArrayDecl(this);
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
        if (iniciales != null && !iniciales.isEmpty()) {
            if (iniciales.size() != tamano) {
                ctx.registrarError(getLine(), getColumn(),
                    "El arreglo declara tamaño " + tamano + " pero el inicializador tiene "
                    + iniciales.size() + " elementos");
            }
            for (Node valor : iniciales) {
                Type tipoValor = ctx.evaluar(valor);
                if (!TypeCompat.esAsignable(tipo, tipoValor)) {
                    ctx.registrarError(valor.getLine(), valor.getColumn(),
                        "Elemento de tipo " + tipoValor + " no es compatible con arreglo de " + tipo);
                }
            }
        }

        Symbol simbolo = new Symbol(nombre, tipo, Symbol.Kind.VARIABLE, true, false, false, tamano);
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            simbolo.setTipoNombre(tipoNombre);
        }
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "El arreglo '" + nombre + "' ya fue declarado");
        }
        return tipo;
    }
}