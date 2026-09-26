package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class ConstructorDeclNode extends Sentencia {
    public List<ParameterNode> parametros;
    public BlockNode cuerpo;
    private final int line;
    private final int column;

    public ConstructorDeclNode(List<ParameterNode> parametros, BlockNode cuerpo, int line, int column) {
        this.parametros = parametros;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        int idx = gen.nextConstructorIndex();
        String nombreCompuesto = gen.currentClassName() + "_init_" + idx;
        String firma = "self:" + gen.currentClassName()
                + (parametros.isEmpty() ? "" : "; " + ParameterNode.descriptores(parametros));
        ctx.agregar(new InicioFuncion3D(nombreCompuesto, firma, gen.currentClassName(), "void"));
        cuerpo.traducir(ctx);
        ctx.agregar(new FinFuncion3D(nombreCompuesto));
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarAmbitoDentroDe(ctx.ambitoDeClaseActual());
        for (ParameterNode p : parametros) ctx.evaluar(p);

        ctx.pushTipoRetorno(null);
        ctx.evaluar(cuerpo);
        ctx.popTipoRetorno();

        ctx.salirAmbito();
        return Type.VOID;
    }
}
