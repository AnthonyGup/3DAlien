package cunoc.compi2.alien_code.ast.stmt;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.access.NameAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.List;

public class ArrayDeclNode extends Sentencia {
    public String nombre;
    public int tamano;
    public int dimensiones = 1;
    public String dims;
    public Type tipo;
    public String tipoNombre;
    public List<Node> iniciales;
    private final int line;
    private final int column;

    public ArrayDeclNode(String nombre, int tamano, Type tipo, String tipoNombre, List<Node> iniciales, int line, int column) {
        this.nombre = nombre;
        this.tamano = tamano;
        this.tipo = tipo;
        this.tipoNombre = tipoNombre;
        this.iniciales = iniciales;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }


    @Override
    public void traducir(CodigoContexto ctx) {
        String dimsTexto = dims != null ? dims : String.valueOf(tamano);
        String tipoTexto = (tipo == Type.STRUCT || tipo == Type.CLASS) && tipoNombre != null
                ? tipoNombre : tipo.name();
        ctx.agregar(new DeclararArreglo3D(nombre, tipoTexto, dimsTexto));
        if (iniciales != null) {
            MemoryAccess base = new NameAccess(nombre, tipo, tipoNombre, false, dimensiones);
            for (int i = 0; i < iniciales.size(); i++) {
                MemoryAccess val = ((Expresion) iniciales.get(i)).traducir(ctx);
                if (val instanceof Literal3D literal) {
                    val = Operandos.literalBraces(base, literal);
                }
                MemoryAccess indice = new IndiceAccess(base, new Literal3D(String.valueOf(i), Type.INT),
                        tipo, tipoNombre, Math.max(0, dimensiones - 1));
                ctx.agregar(new Asignacion3D(indice, val));
            }
        }
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        if (iniciales != null && !iniciales.isEmpty()) {
            if (iniciales.size() != tamano) {
                ctx.registrarError(getLine(), getColumn(),
                    "El arreglo declara tamaño " + tamano + " pero el inicializador tiene "
                    + iniciales.size() + " elementos");
            }
            for (Node valor : iniciales) {
                Type tipoValor = ctx.evaluar(valor);
                if (!TypeCompat.esAsignable(tipo, tipoValor)) {
                    ctx.registrarError(valor.getLine(), valor.getColumn(),
                        "Elemento de tipo " + tipoValor + " no es compatible con arreglo de " + tipo);
                }
            }
        }

        Symbol simbolo = new Symbol(nombre, tipo, Symbol.Kind.VARIABLE, true, false, false, tamano);
        simbolo.setDimensiones(dimensiones);
        simbolo.setLinea(getLine());
        simbolo.setColumna(getColumn());
        if (tipo == Type.STRUCT || tipo == Type.CLASS) {
            simbolo.setTipoNombre(tipoNombre);
        }
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(getLine(), getColumn(), "El arreglo '" + nombre + "' ya fue declarado");
        }
        return tipo;
    }
}