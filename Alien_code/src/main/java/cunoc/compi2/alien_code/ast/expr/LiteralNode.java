package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

public class LiteralNode extends Expresion {
    public enum Clase { ENTERO, DECIMAL, CADENA, CARACTER, BOOLEANO, NULO }

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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        switch (clase) {
            case ENTERO: return new Literal3D(valor, Type.INT);
            case DECIMAL: return new Literal3D(valor, Type.FLOAT);
            case CADENA: return new Literal3D(valor, Type.STRING);
            case CARACTER: return new Literal3D(valor, Type.CHAR);
            case BOOLEANO: {
                boolean cierto = valor != null && (valor.equals("true") || valor.equals("1")
                        || valor.equals("verum") || valor.equals("verdadero"));
                return new Literal3D(cierto ? "1" : "0", Type.BOOL);
            }
            case NULO: return new Literal3D("NULL", Type.NULL);
            default: return new Literal3D("0", Type.INT);
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        switch (clase) {
            case ENTERO: return Type.INT;
            case DECIMAL: return Type.FLOAT;
            case CADENA: return Type.STRING;
            case CARACTER: return Type.CHAR;
            case BOOLEANO: return Type.BOOL;
            case NULO: return Type.NULL;
            default: return null;
        }
    }
}