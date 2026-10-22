package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Sentencia;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.expr.AccessNode;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ast.stmt.SuperCallNode;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.NombresInternos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

public class ConstructorDeclNode extends Sentencia {
    public List<ParameterNode> parametros;
    public BlockNode cuerpo;
    public Visibilidad visibilidad = Visibilidad.DEFAULT;
    private final int line;
    private final int column;

    public ConstructorDeclNode(List<ParameterNode> parametros, BlockNode cuerpo, int line, int column) {
        this.parametros = parametros;
        this.cuerpo = cuerpo;
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    @Override
    public void traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        int idx = gen.nextConstructorIndex();
        String clase = gen.currentClassName();
        String nombreCompuesto = clase + "_init_" + idx;
        String firma = "self:" + clase
                + (parametros.isEmpty() ? "" : "; " + ParameterNode.descriptores(parametros));
        ctx.agregar(new InicioFuncion3D(nombreCompuesto, firma, clase, "void"));
        emitirCadenaSuper(ctx, gen, clase);
        ConstructorSupport.emitirInicializadores(ctx, gen, clase);
        cuerpo.traducir(ctx);
        ctx.agregar(new FinFuncion3D(nombreCompuesto));
    }

    private void emitirCadenaSuper(CodigoContexto ctx, IntermediateCodeGenerator gen, String clase) {
        Symbol claseSimbolo = gen.getLayouts() != null ? gen.getLayouts().simboloDe(clase) : null;
        Symbol padre = claseSimbolo != null ? claseSimbolo.getSuperclaseResuelta() : null;
        if (padre == null) {
            return;
        }
        SuperCallNode superLlamada = primeraSuper();
        List<Node> args = superLlamada != null ? superLlamada.argumentos : java.util.Collections.emptyList();
        int argCount = args.size();
        Symbol ctorPadre = ConstructorSupport.buscarConstructor(padre, argCount);
        int idxPadre = NombresInternos.indiceConstructor(padre, ctorPadre);
        String etiquetaPadre = NombresInternos.constructor(padre.getName(), idxPadre);

        List<cunoc.compi2.alien_code.c3d.access.MemoryAccess> argTraducidos = new java.util.ArrayList<>();
        argTraducidos.add(new cunoc.compi2.alien_code.c3d.access.Literal3D("self", Type.CLASS));
        for (Node a : args) {
            argTraducidos.add(((cunoc.compi2.alien_code.ast.Expresion) a).traducir(ctx));
        }
        ctx.agregar(new cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D(etiquetaPadre, argTraducidos, null));
    }

    private SuperCallNode primeraSuper() {
        for (Node s : cuerpo.sentencias) {
            if (s instanceof SuperCallNode sup) {
                return sup;
            }
        }
        return null;
    }

    @Override
    public Type analizar(ContextoSemantico ctx) {
        ctx.entrarAmbitoDentroDe(ctx.ambitoDeClaseActual());
        for (ParameterNode p : parametros) ctx.evaluar(p);

        ctx.pushTipoRetorno(null);
        ctx.entrarRutina(true);
        validarSuper(ctx);
        ctx.evaluar(cuerpo);
        ctx.salirRutina();
        ctx.popTipoRetorno();

        ctx.salirAmbito();
        return Type.VOID;
    }

    private void validarSuper(ContextoSemantico ctx) {
        Symbol clase = ctx.claseActual();
        Symbol padre = clase != null ? clase.getSuperclaseResuelta() : null;

        int ocurrencias = 0;
        SuperCallNode primero = null;
        int indice = 0;
        for (Node s : cuerpo.sentencias) {
            if (s instanceof SuperCallNode sup) {
                ocurrencias++;
                if (ocurrencias == 1 && indice != 0) {
                    ctx.registrarError(sup.getLine(), sup.getColumn(),
                        "super(...) debe ser la primera instrucción del constructor");
                }
                if (ocurrencias > 1) {
                    ctx.registrarError(sup.getLine(), sup.getColumn(),
                        "super(...) debe ser la primera instrucción del constructor");
                }
                if (primero == null) primero = sup;
            }
            indice++;
        }

        if (padre == null) {
            if (ocurrencias > 0) {
                ctx.registrarError(primero.getLine(), primero.getColumn(),
                    "super(...) solo puede usarse en constructores de una clase que hereda");
            }
            return;
        }

        if (ocurrencias == 0) {
            boolean tieneVacio = false;
            for (Symbol ctor : padre.getConstructores()) {
                if (ctor.getTiposParametros().isEmpty()) { tieneVacio = true; break; }
            }
            if (padre.getConstructores().isEmpty()) tieneVacio = true;
            if (!tieneVacio) {
                ctx.registrarError(getLine(), getColumn(),
                    "'" + padre.getName() + "' no tiene constructor sin parámetros: llame a super(...)");
            }
            return;
        }

        List<Type> tiposArgs = AccessNode.tiposDeArgumentos(ctx, primero.argumentos);
        boolean coincide = false;
        for (Symbol ctor : padre.getConstructores()) {
            if (firmaCoincide(ctor.getTiposParametros(), tiposArgs)) { coincide = true; break; }
        }
        if (padre.getConstructores().isEmpty() && tiposArgs.isEmpty()) {
            coincide = true;
        }
        if (!coincide) {
            StringBuilder tipos = new StringBuilder();
            for (Type t : tiposArgs) {
                if (tipos.length() > 0) tipos.append(", ");
                tipos.append(t);
            }
            ctx.registrarError(primero.getLine(), primero.getColumn(),
                "Ningún constructor de '" + padre.getName() + "' acepta (" + tipos + ")");
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
