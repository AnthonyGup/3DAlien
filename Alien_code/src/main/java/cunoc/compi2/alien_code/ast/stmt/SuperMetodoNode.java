package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.POO;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

public class SuperMetodoNode extends Sentencia {

    public String metodo;
    public List<Node> argumentos;
    private Symbol resultado;
    private final int line;
    private final int column;

    public SuperMetodoNode(String metodo, List<Node> argumentos, int line, int column) {
        this.metodo = metodo;
        this.argumentos = argumentos;
        this.line = line;
        this.column = column;
    }

    @Override
    public void traducir(CodigoContexto ctx) {
        if (resultado == null || !(ctx instanceof IntermediateCodeGenerator gen)) {
            return;
        }
        List<MemoryAccess> args = new ArrayList<>();
        for (Node a : argumentos) {
            args.add(((Expresion) a).traducir(ctx));
        }
        args.add(0, new Literal3D("self", Type.CLASS));
        int idx = gen.overloadIndexByArgCount(resultado.getFirmas(), argumentos.size());
        String duena = resultado.getClaseDuena() != null
                ? resultado.getClaseDuena() : gen.currentClassName();
        ctx.agregar(new Llamada3D(duena + "_" + resultado.getName() + "_" + idx, args, null));
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        Symbol clase = ctx.claseActual();
        if (clase == null) {
            ctx.registrarError(getLine(), getColumn(),
                "'super' solo puede usarse dentro de una clase");
            return null;
        }
        Symbol padre = clase.getSuperclaseResuelta();
        if (padre == null) {
            ctx.registrarError(getLine(), getColumn(),
                "'super' requiere que '" + clase.getName() + "' herede de otra clase");
            return null;
        }
        Symbol objetivo = POO.buscarMetodo(padre, metodo);
        if (objetivo == null) {
            ctx.registrarError(getLine(), getColumn(),
                "La superclase de '" + clase.getName() + "' no tiene un método '" + metodo + "'");
            return null;
        }
        if (!POO.puedeAcceder(objetivo, clase)) {
            String duena = objetivo.getClaseDuena() != null ? objetivo.getClaseDuena() : "?";
            ctx.registrarError(getLine(), getColumn(),
                "'" + metodo + "' es privado en '" + duena + "'");
            return null;
        }
        List<Type> tiposArgs = AccessNode.tiposDeArgumentos(ctx, argumentos);
        if (!objetivo.tieneFirmaCompatible(tiposArgs)) {
            ctx.registrarError(getLine(), getColumn(),
                "El método '" + metodo + "' no coincide con los argumentos dados");
            return null;
        }
        resultado = objetivo;
        return objetivo.getType();
    }
}
