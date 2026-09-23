package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class NewObjectNode implements Node {
    public String nombreClase;
    public List<Node> argumentos;
    private final int line;
    private final int column;

    public NewObjectNode(String nombreClase, List<Node> argumentos, int line, int column) {
        this.nombreClase = nombreClase;
        this.argumentos = argumentos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitNewObject(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String t = ctx.nuevoTemporal();
        gen.recordTemporal(t, Type.CLASS, false, 0, nombreClase);
        ctx.emitir("call", "malloc", "sizeof(struct " + nombreClase + ")", t);

        StringBuilder args = new StringBuilder(t);
        for (Node a : argumentos) {
            args.append(", ").append(a.traducir(ctx));
        }

        int overloadIndex = resolveConstructorOverloadIndex(gen);
        String discard = ctx.nuevoTemporal();
        gen.recordTemporalType(discard, Type.VOID);
        ctx.emitir("call", nombreClase + "_init_" + overloadIndex, args.toString(), discard);
        return t;
    }

    private int resolveConstructorOverloadIndex(IntermediateCodeGenerator gen) {
        Symbol clase = gen.resolveForTranslation(nombreClase);
        if (clase == null) {
            return 0;
        }
        return gen.overloadIndexByArgCount(clase.getFirmasConstructores(), argumentos.size());
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
        List<Type> tiposArgs = AccessNode.tiposDeArgumentos(ctx, argumentos);

        if (!clase.tieneConstructorCompatible(tiposArgs)) {
            ctx.registrarError(getLine(), getColumn(),
                "No existe un constructor de '" + nombreClase + "' que reciba esos argumentos");
            return null;
        }
        return Type.CLASS;
    }
}