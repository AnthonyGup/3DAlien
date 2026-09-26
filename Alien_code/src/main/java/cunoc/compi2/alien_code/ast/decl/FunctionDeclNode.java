package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class FunctionDeclNode extends Sentencia {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        String ret = Operandos.tipoRetornoC(tipoRetorno, tipoRetornoNombre);
        ctx.agregar(new InicioFuncion3D(nombre, ParameterNode.descriptores(parametros), null, ret));
        cuerpo.traducir(ctx);
        ctx.agregar(new FinFuncion3D(nombre));
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
