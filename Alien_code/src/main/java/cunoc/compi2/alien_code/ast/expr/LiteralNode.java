package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;

public class LiteralNode implements Node {
    public enum Clase { ENTERO, DECIMAL, CADENA, CARACTER, BOOLEANO }

    public Clase clase;
    public String valor;
    private final int line;
    private final int column;

    public LiteralNode(Clase clase, String valor, int line, int column) {
        this.clase = clase;
        this.valor = valor;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLiteral(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        switch (clase) {
            case ENTERO: return Type.INT;
            case DECIMAL: return Type.FLOAT;
            case CADENA: return Type.STRING;
            case CARACTER: return Type.CHAR;
            case BOOLEANO: return Type.BOOL;
            default: return null;
        }
    }
}