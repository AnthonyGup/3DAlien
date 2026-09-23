package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class NewArrayNode implements Node {
    public Type tipoElemento;
    public String tipoNombreElemento;
    public List<Node> dimensiones;
    private final int line;
    private final int column;

    public NewArrayNode(Type tipoElemento, List<Node> dimensiones, int line, int column) {
        this(tipoElemento, null, dimensiones, line, column);
    }

    public NewArrayNode(Type tipoElemento, String tipoNombreElemento, List<Node> dimensiones, int line, int column) {
        this.tipoElemento = tipoElemento;
        this.tipoNombreElemento = tipoNombreElemento;
        this.dimensiones = dimensiones;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitNewArray(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        StringBuilder dims = new StringBuilder();
        for (int i = 0; i < dimensiones.size(); i++) {
            if (i > 0) dims.append(", ");
            dims.append(dimensiones.get(i).traducir(ctx));
        }
        String t = ctx.nuevoTemporal();
        ((IntermediateCodeGenerator) ctx)
                .recordTemporal(t, tipoElemento, true, dimensiones.size(), tipoNombreElemento);
        ctx.emitir("newarray", dims.toString(), null, t);
        return t;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node dim : dimensiones) {
            Type t = ctx.evaluar(dim);
            if (t != Type.INT) {
                ctx.registrarError(getLine(), getColumn(), "El tamaño de un arreglo debe ser entero, se dio " + t);
            }
        }
        return Type.ARRAY;
    }
}
