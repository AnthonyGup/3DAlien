package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class MethodDeclNode extends Sentencia {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        int idx = gen.nextMethodIndex(nombre);
        String nombreCompuesto = gen.currentClassName() + "_" + nombre + "_" + idx;
        String firma = "self:" + gen.currentClassName()
                + (parametros.isEmpty() ? "" : "; " + ParameterNode.descriptores(parametros));
        String ret = Operandos.tipoRetornoC(tipoRetorno, tipoRetornoNombre);
        ctx.agregar(new InicioFuncion3D(nombreCompuesto, firma, gen.currentClassName(), ret));
        cuerpo.traducir(ctx);
        ctx.agregar(new FinFuncion3D(nombreCompuesto));
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
