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
    public String tipoDeclarado;
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
        String etiquetaCtor = cunoc.compi2.alien_code.ir.NombresInternos.constructor(nombreClase, overloadIndex);
        ctx.agregar(new Llamada3D(etiquetaCtor, argumentosTraducidos, null));
        return t;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Symbol clase = ctx.resolver(nombreClase);
        if (clase == null || clase.getKind() != Symbol.Kind.CLASE) {
            ctx.registrarError(getLine(), getColumn(),
                "La clase '" + nombreClase + "' no existe");
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
        verificarAccesoConstructor(ctx, clase, tiposArgs);
        return Type.CLASS;
    }

    private void verificarAccesoConstructor(ContextoSemantico ctx, Symbol clase, List<Type> tiposArgs) {
        Symbol contexto = ctx.claseActual();
        for (Symbol ctor : clase.getConstructores()) {
            if (firmaCoincide(ctor.getTiposParametros(), tiposArgs)) {
                if (!cunoc.compi2.alien_code.semantic.POO.puedeAcceder(ctor, contexto)) {
                    ctx.registrarError(getLine(), getColumn(),
                        "El constructor de '" + clase.getName() + "' no es accesible desde aquí");
                }
                return;
            }
        }
    }

    private boolean firmaCoincide(List<Type> params, List<Type> args) {
        if (params.size() != args.size()) return false;
        for (int i = 0; i < params.size(); i++) {
            if (!cunoc.compi2.alien_code.semantic.TypeCompat.esAsignable(params.get(i), args.get(i))) {
                return false;
            }
        }
        return true;
    }
}