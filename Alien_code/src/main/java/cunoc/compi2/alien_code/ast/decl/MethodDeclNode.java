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

public class MethodDeclNode implements Node {
    public String nombre;
    public List<ParameterNode> parametros;
    public Type tipoRetorno;
    public String tipoRetornoNombre;
    public Type tipoRetornoElemento;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public MethodDeclNode(String nombre, List<ParameterNode> parametros, Type tipoRetorno, BlockNode cuerpo, int line, int column) {
        this.nombre = nombre;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitMethodDecl(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        int idx = gen.nextMethodIndex(nombre);
        ctx.emitir("func", gen.currentClassName() + "_" + nombre + "_" + idx,
                String.valueOf(parametros.size()), null);
        gen.entrarAmbitoTraduccion(gen.currentClassMembers());
        for (ParameterNode p : parametros) {
            gen.definirEnTraduccion(Symbol.variable(p.nombre, p.tipo, p.tipoElemento, p.tipoNombre,
                    p.dimensiones, true, false, p.getLine(), p.getColumn()));
        }
        cuerpo.traducir(ctx);
        gen.salirAmbitoTraduccion();
        ctx.emitir("func_end", gen.currentClassName() + "_" + nombre + "_" + idx, null, null);
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarAmbitoDentroDe(ctx.ambitoDeClaseActual());
        for (ParameterNode p : parametros) ctx.evaluar(p);

        Type esperado = tipoRetorno == Type.ARRAY && tipoRetornoElemento != null
            ? tipoRetornoElemento : tipoRetorno;
        ctx.pushTipoRetorno(esperado);
        ctx.evaluar(cuerpo);
        ctx.popTipoRetorno();

        ctx.salirAmbito();
        return tipoRetorno;
    }
}
