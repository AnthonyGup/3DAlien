package cunoc.compi2.alien_code.ast.program;
import cunoc.compi2.alien_code.ast.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class ImportNode implements Node {
    public String rutaCompleta;
    private final int line;
    private final int column;

    public ImportNode(String rutaCompleta, int line, int column) {
        this.rutaCompleta = rutaCompleta;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitImport(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }
}