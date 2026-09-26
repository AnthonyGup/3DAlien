package cunoc.compi2.alien_code.ast.program;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class ImportNode extends Sentencia {
    public String rutaCompleta;
    private final int line;
    private final int column;

    public ImportNode(String rutaCompleta, int line, int column) {
        this.rutaCompleta = rutaCompleta;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        return Type.VOID;
    }
}