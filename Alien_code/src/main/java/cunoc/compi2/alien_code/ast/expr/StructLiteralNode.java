package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.ArrayList;
import java.util.List;

public class StructLiteralNode extends Expresion {
    public List<Node> valores;
    private final int line;
    private final int column;

    public StructLiteralNode(List<Node> valores, int line, int column) {
        this.valores = valores;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(Operandos.texto(((Expresion) valores.get(i)).traducir(ctx)));
        }
        return new Literal3D("{" + sb + "}", Type.STRUCT);
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node valor : valores) {
            ctx.evaluar(valor);
        }
        return Type.STRUCT;
    }

    public void validarContra(Symbol estructura, ContextoSemantico ctx) {
        if (estructura == null || estructura.getMiembros() == null) {
            ctx.registrarError(getLine(), getColumn(), "Literal de estructura sin tipo de estructura conocido");
            return;
        }
        List<Symbol> campos = new ArrayList<>(estructura.getMiembros().getTodos());
        if (valores.size() != campos.size()) {
            ctx.registrarError(getLine(), getColumn(),
                "'" + estructura.getName() + "' espera " + campos.size()
                + " valores, se dieron " + valores.size());
            return;
        }
        for (int i = 0; i < valores.size(); i++) {
            Node valor = valores.get(i);
            Symbol campoEsperado = campos.get(i);
            if (valor instanceof StructLiteralNode && campoEsperado.getType() == Type.STRUCT) {
                Symbol tipoAnidado = ctx.resolver(campoEsperado.getTipoNombre());
                ((StructLiteralNode) valor).validarContra(tipoAnidado, ctx);
            } else {
                Type tipoValor = ctx.evaluar(valor);
                if (!TypeCompat.esAsignable(campoEsperado.getType(), tipoValor)) {
                    ctx.registrarError(valor.getLine(), valor.getColumn(),
                        "El campo #" + (i + 1) + " de '" + estructura.getName() + "' espera "
                        + campoEsperado.getType() + " pero se dio " + tipoValor);
                }
            }
        }
    }
}