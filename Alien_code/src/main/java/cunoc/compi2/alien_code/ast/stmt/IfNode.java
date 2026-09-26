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
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.List;

public class IfNode extends Sentencia {
    public Node condicion;
    public BlockNode cuerpo;
    public List<ElseIfNode> ramas;
    private final int line;
    private final int column;

    public IfNode(Node condicion, BlockNode cuerpo, List<ElseIfNode> ramas, int line, int column) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.ramas = ramas;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        MemoryAccess cond = ((Expresion) condicion).traducir(ctx);
        LabelAccess end = Operandos.etiqueta(ctx);
        LabelAccess elseLabel = ramas.isEmpty() ? end : Operandos.etiqueta(ctx);
        ctx.agregar(new Condicional3D(false, cond, elseLabel));

        if (cuerpo != null) {
            cuerpo.traducir(ctx);
        }

        if (ramas.isEmpty()) {
            ctx.agregar(new Etiqueta3D(end));
            return;
        }

        ctx.agregar(new Goto3D(end));
        ctx.agregar(new Etiqueta3D(elseLabel));
        for (int i = 0; i < ramas.size(); i++) {
            ElseIfNode rama = ramas.get(i);
            if (i == ramas.size() - 1 && rama.esElse()) {
                rama.cuerpo.traducir(ctx);
            } else {
                LabelAccess next = Operandos.etiqueta(ctx);
                MemoryAccess condRama = ((Expresion) rama.condicion).traducir(ctx);
                ctx.agregar(new Condicional3D(false, condRama, next));
                rama.cuerpo.traducir(ctx);
                ctx.agregar(new Goto3D(end));
                ctx.agregar(new Etiqueta3D(next));
            }
        }
        ctx.agregar(new Etiqueta3D(end));
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Type tipoCondicion = ctx.evaluar(condicion);
        if (tipoCondicion != null && tipoCondicion != Type.BOOL) {
            ctx.registrarError(getLine(), getColumn(), "La condición debe ser booleana, se dio " + tipoCondicion);
        }
        ctx.entrarAmbito();
        ctx.evaluar(cuerpo);
        ctx.salirAmbito();

        for (ElseIfNode rama : ramas) {
            ctx.evaluar(rama);
        }
        return Type.VOID;
    }
}