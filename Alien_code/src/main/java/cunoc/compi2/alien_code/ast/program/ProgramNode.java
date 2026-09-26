package cunoc.compi2.alien_code.ast.program;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class ProgramNode extends Sentencia {
    public List<Node> declarations;
    public String sourceLanguage;
    private int line;
    private int column;

    public ProgramNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        for (Node declaracion : declarations) {
            Operandos.ejecutar(ctx, declaracion);
        }
        if ("Pig Latin".equals(sourceLanguage)) {
            ctx.agregar(new Halt3D());
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node declaracion : declarations) {
            ctx.evaluar(declaracion);
        }
        return Type.VOID;
    }
}
