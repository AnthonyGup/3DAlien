package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class FunctionDeclNode implements Node {
    public String nombre;
    public List<ParameterNode> parametros;
    public Type tipoRetorno;
    public String tipoRetornoNombre;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public FunctionDeclNode(String nombre, List<ParameterNode> parametros, Type tipoRetorno, BlockNode cuerpo, int line, int column) {
        this.nombre = nombre;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunctionDecl(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        ctx.emitir("func", nombre, String.valueOf(parametros.size()), null);
        gen.entrarAmbitoTraduccion();
        for (ParameterNode p : parametros) {
            gen.definirEnTraduccion(Symbol.variable(p.nombre, p.tipo, p.tipoElemento, p.tipoNombre,
                    p.dimensiones, true, false, p.getLine(), p.getColumn()));
        }
        cuerpo.traducir(ctx);
        gen.salirAmbitoTraduccion();
        ctx.emitir("func_end", nombre, null, null);
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarAmbito();
        for (ParameterNode p : parametros) {
            ctx.evaluar(p);
        }

        ctx.pushTipoRetorno(tipoRetorno);
        ctx.evaluar(cuerpo);
        ctx.popTipoRetorno();

        ctx.salirAmbito();
        return tipoRetorno;
    }
}
