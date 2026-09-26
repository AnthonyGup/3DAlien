package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.ArrayList;
import java.util.List;

public class NewObjectNode extends Expresion {
    public String nombreClase;
    public List<Node> argumentos;
    private Symbol claseResuelta;
    private final int line;
    private final int column;

    public NewObjectNode(String nombreClase, List<Node> argumentos, int line, int column) {
        this.nombreClase = nombreClase;
        this.argumentos = argumentos;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        MemoryAccess t = Operandos.temporal(ctx, "struct " + nombreClase + " *");
        ctx.agregar(new Asignacion3D(t, new Literal3D("malloc(sizeof(struct " + nombreClase + "))", Type.CLASS)));

        List<MemoryAccess> argumentosTraducidos = new ArrayList<>();
        argumentosTraducidos.add(t);
        for (Node a : argumentos) {
            argumentosTraducidos.add(((Expresion) a).traducir(ctx));
        }

        int overloadIndex = claseResuelta != null
                ? gen.overloadIndexByArgCount(claseResuelta.getFirmasConstructores(), argumentos.size())
                : 0;
        ctx.agregar(new Llamada3D(nombreClase + "_init_" + overloadIndex, argumentosTraducidos, null));
        return t;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Symbol clase = ctx.resolver(nombreClase);
        if (clase == null || clase.getKind() != Symbol.Kind.CLASE) {
            ctx.registrarError(getLine(), getColumn(),
                "'" + nombreClase + "' no es una clase conocida (¿falta un import .z?)");
            for (Node a : argumentos) ctx.evaluar(a);
            return null;
        }
        claseResuelta = clase;
        List<Type> tiposArgs = AccessNode.tiposDeArgumentos(ctx, argumentos);

        if (!clase.tieneConstructorCompatible(tiposArgs)) {
            ctx.registrarError(getLine(), getColumn(),
                "No existe un constructor de '" + nombreClase + "' que reciba esos argumentos");
            return null;
        }
        return Type.CLASS;
    }
}