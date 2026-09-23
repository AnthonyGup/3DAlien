package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.ASTVisitor;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.ArrayList;
import java.util.List;

public class SwitchNode implements Node {
    public Node expresion;
    public List<CaseNode> casos;
    private final int line;
    private final int column;

    public SwitchNode(Node expresion, List<CaseNode> casos, int line, int column) {
        this.expresion = expresion;
        this.casos = casos;
        this.line = line;
        this.column = column;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitSwitch(this);
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String v = expresion.traducir(ctx);
        String end = ctx.nuevaEtiqueta();
        String defLabel = ctx.nuevaEtiqueta();
        List<String> labels = new ArrayList<>();
        boolean hayDefault = false;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                hayDefault = true;
            } else {
                labels.add(ctx.nuevaEtiqueta());
            }
        }

        ctx.empujarCiclo(null, end);
        int indice = 0;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                continue;
            }
            String valor = caso.valor.traducir(ctx);
            String igual = ctx.nuevoTemporal();
            gen.recordTemporalType(igual, Type.BOOL);
            ctx.emitir("==", v, valor, igual);
            ctx.emitir("if_true", igual, null, labels.get(indice++));
        }
        if (hayDefault) {
            ctx.emitir("goto", null, null, defLabel);
        } else {
            ctx.emitir("goto", null, null, end);
        }

        indice = 0;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                ctx.emitir("label", null, null, defLabel);
            } else {
                ctx.emitir("label", null, null, labels.get(indice++));
            }
            for (Node sentencia : caso.sentencias) {
                sentencia.traducir(ctx);
            }
        }
        ctx.emitir("label", null, null, end);
        ctx.popCiclo();
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.evaluar(expresion);
        ctx.entrarSwitch();
        for (CaseNode caso : casos) {
            ctx.evaluar(caso);
        }
        ctx.salirSwitch();
        return Type.VOID;
    }
}
