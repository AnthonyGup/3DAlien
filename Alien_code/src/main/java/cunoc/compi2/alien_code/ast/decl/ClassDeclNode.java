package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class ClassDeclNode extends Sentencia {
    public String nombre;
    public List<VariableDeclNode> atributos;
    public List<ConstructorDeclNode> constructores;
    public List<MethodDeclNode> metodos;
    private final int line;
    private final int column;

    public ClassDeclNode(String nombre, List<VariableDeclNode> atributos, List<ConstructorDeclNode> constructores,
            List<MethodDeclNode> metodos, int line, int column) {
        this.nombre = nombre;
        this.atributos = atributos;
        this.constructores = constructores;
        this.metodos = metodos;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        ctx.pushClassTranslation(nombre);
        for (ConstructorDeclNode c : constructores) c.traducir(ctx);
        for (MethodDeclNode m : metodos) m.traducir(ctx);
        ctx.popClassTranslation();
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (VariableDeclNode attr : atributos) {
            if (attr.tipo == Type.STRUCT || attr.tipo == Type.CLASS) {
                Symbol t = ctx.resolver(attr.tipoNombre);
                if (t == null) {
                    ctx.registrarError(attr.getLine(), attr.getColumn(),
                        "El atributo '" + attr.nombre + "' usa el tipo desconocido '" + attr.tipoNombre + "'");
                }
            }
        }

        Symbol propio = ctx.resolver(nombre);
        ctx.pushClaseActual(propio.getMiembros());
        for (ConstructorDeclNode c : constructores) ctx.evaluar(c);
        for (MethodDeclNode m : metodos) ctx.evaluar(m);
        ctx.popClaseActual();

        return Type.VOID;
    }
}
