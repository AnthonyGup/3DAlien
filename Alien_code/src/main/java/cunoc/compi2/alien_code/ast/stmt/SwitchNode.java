package cunoc.compi2.alien_code.ast.stmt;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.ArrayList;
import java.util.List;

public class SwitchNode extends Sentencia {
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
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        MemoryAccess v = ((Expresion) expresion).traducir(ctx);
        LabelAccess end = Operandos.etiqueta(ctx);
        LabelAccess defLabel = Operandos.etiqueta(ctx);
        List<LabelAccess> labels = new ArrayList<>();
        boolean hayDefault = false;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                hayDefault = true;
            } else {
                labels.add(Operandos.etiqueta(ctx));
            }
        }

        ctx.empujarCiclo(null, end.getNombre());
        int indice = 0;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                continue;
            }
            MemoryAccess valor = ((Expresion) caso.valor).traducir(ctx);
            MemoryAccess igual = Operandos.temporal(ctx, "int");
            ctx.agregar(new Operacion3D("==", v, valor, igual));
            ctx.agregar(new Condicional3D(true, igual, labels.get(indice++)));
        }
        if (hayDefault) {
            ctx.agregar(new Goto3D(defLabel));
        } else {
            ctx.agregar(new Goto3D(end));
        }

        indice = 0;
        for (CaseNode caso : casos) {
            if (caso.esSiempre()) {
                ctx.agregar(new Etiqueta3D(defLabel));
            } else {
                ctx.agregar(new Etiqueta3D(labels.get(indice++)));
            }
            for (Node sentencia : caso.sentencias) {
                Operandos.ejecutar(ctx, sentencia);
            }
        }
        ctx.agregar(new Etiqueta3D(end));
        ctx.popCiclo();
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
