package cunoc.compi2.alien_code.ast.expr;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.TiposC;
import cunoc.compi2.alien_code.c3d.access.LabelAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;

import java.util.ArrayList;
import java.util.List;

public class NewArrayNode extends Expresion {
    public Type tipoElemento;
    public String tipoNombreElemento;
    public List<Node> dimensiones;
    private final int line;
    private final int column;

    public NewArrayNode(Type tipoElemento, List<Node> dimensiones, int line, int column) {
        this(tipoElemento, null, dimensiones, line, column);
    }

    public NewArrayNode(Type tipoElemento, String tipoNombreElemento, List<Node> dimensiones, int line, int column) {
        this.tipoElemento = tipoElemento;
        this.tipoNombreElemento = tipoNombreElemento;
        this.dimensiones = dimensiones;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        List<MemoryAccess> dims = new ArrayList<>();
        for (Node dim : dimensiones) {
            dims.add(((Expresion) dim).traducir(ctx));
        }
        String elemWord = elemWord();
        String elemC = TiposC.elementTypeNew(elemWord);
        MemoryAccess t;
        if (dims.size() == 1) {
            t = Operandos.temporal(ctx, elemC + "*");
            ctx.agregar(new Asignacion3D(t, new Literal3D(
                    "malloc(" + Operandos.texto(dims.get(0)) + " * " + TiposC.mallocSize(elemWord) + ")",
                    Type.ARRAY)));
        } else if (dims.size() == 2) {
            t = Operandos.temporal(ctx, elemC + "**");
            String filas = Operandos.texto(dims.get(0));
            String cols = Operandos.texto(dims.get(1));
            ctx.agregar(new Asignacion3D(t, new Literal3D(
                    "malloc(" + filas + " * " + TiposC.mallocSizePtr(elemWord) + ")", Type.ARRAY)));
            MemoryAccess contador = Operandos.temporal(ctx, "int");
            ctx.agregar(new Asignacion3D(contador, new Literal3D("0", Type.INT)));
            LabelAccess etiq1 = Operandos.etiqueta(ctx);
            LabelAccess etiq2 = Operandos.etiqueta(ctx);
            ctx.agregar(new Etiqueta3D(etiq1));
            ctx.agregar(new Condicional3D(false,
                    new Literal3D(Operandos.texto(contador) + " < " + filas, Type.BOOL), etiq2));
            MemoryAccess fila = new IndiceAccess(t, contador, tipoElemento, tipoNombreElemento, 0);
            ctx.agregar(new Asignacion3D(fila, new Literal3D(
                    "malloc(" + cols + " * " + TiposC.mallocSize(elemWord) + ")", Type.ARRAY)));
            ctx.agregar(new Operacion3D("+", contador, new Literal3D("1", Type.INT), contador));
            ctx.agregar(new Goto3D(etiq1));
            ctx.agregar(new Etiqueta3D(etiq2));
        } else {
            StringBuilder producto = new StringBuilder();
            for (int i = 0; i < dims.size(); i++) {
                if (i > 0) producto.append(" * ");
                producto.append(Operandos.texto(dims.get(i)));
            }
            t = Operandos.temporal(ctx, elemC + "*");
            ctx.agregar(new Asignacion3D(t, new Literal3D(
                    "malloc(" + producto + " * " + TiposC.mallocSize(elemWord) + ")", Type.ARRAY)));
        }
        return t;
    }

    private String elemWord() {
        if ((tipoElemento == Type.STRUCT || tipoElemento == Type.CLASS) && tipoNombreElemento != null) {
            return tipoNombreElemento;
        }
        if (tipoElemento == Type.FLOAT) {
            return "double";
        }
        if (tipoElemento == Type.BOOL) {
            return "bool";
        }
        if (tipoElemento == Type.CHAR) {
            return "char";
        }
        if (tipoElemento == Type.STRING) {
            return "string";
        }
        return "int";
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        for (Node dim : dimensiones) {
            Type t = ctx.evaluar(dim);
            if (t != Type.INT) {
                ctx.registrarError(getLine(), getColumn(), "El tamaño de un arreglo debe ser entero, se dio " + t);
            }
        }
        return Type.ARRAY;
    }
}
