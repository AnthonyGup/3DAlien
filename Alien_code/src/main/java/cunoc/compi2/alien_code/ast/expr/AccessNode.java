package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.CampoAccess;
import cunoc.compi2.alien_code.c3d.access.IndiceAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Impresion;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.ArrayList;
import java.util.List;

public class AccessNode extends Expresion {
    public String nombre;
    public List<Sufijo> sufijos;
    private final int line;
    private final int column;

    public AccessNode(String nombre, int line, int column) {
        this.nombre = nombre;
        this.sufijos = new ArrayList<>();
        this.line = line;
        this.column = column;
    }

    @Override
    public int getLine() { return line; }

    @Override
    public int getColumn() { return column; }

    public static class Sufijo {
        public enum Tipo { CAMPO, INDICE, LLAMADA }

        public final Tipo tipo;
        public final String nombreCampo;
        public final Node indice;
        public final List<Node> argumentos;
        public Symbol resuelto;
        public String claseObjetivo;

        private Sufijo(Tipo tipo, String nombreCampo, Node indice, List<Node> argumentos) {
            this.tipo = tipo;
            this.nombreCampo = nombreCampo;
            this.indice = indice;
            this.argumentos = argumentos;
        }

        public static Sufijo campo(String nombreCampo) {
            return new Sufijo(Tipo.CAMPO, nombreCampo, null, null);
        }

        public static Sufijo indice(Node indice) {
            return new Sufijo(Tipo.INDICE, null, indice, null);
        }

        public static Sufijo llamada(List<Node> argumentos) {
            return new Sufijo(Tipo.LLAMADA, null, null, argumentos);
        }
    }


    @Override
    public MemoryAccess traducir(CodigoContexto ctx) {
        Symbol baseSymbol = ctx.resolverSimbolo(nombre);
        MemoryAccess actual = Operandos.nombre(ctx, nombre, baseSymbol);
        String metodoPendiente = null;
        String claseObjetivo = null;
        for (Sufijo s : sufijos) {
            if (s.tipo == Sufijo.Tipo.CAMPO) {
                if (s.resuelto != null && s.resuelto.getKind() == Symbol.Kind.METODO) {
                    metodoPendiente = s.nombreCampo;
                    claseObjetivo = s.claseObjetivo;
                } else {
                    Symbol miembro = s.resuelto;
                    actual = new CampoAccess(actual, s.nombreCampo, Operandos.tipoDe(miembro),
                            miembro != null ? miembro.getTipoNombre() : null,
                            miembro != null ? miembro.getDimensiones() : 0);
                }
            } else if (s.tipo == Sufijo.Tipo.INDICE) {
                MemoryAccess indice = ((Expresion) s.indice).traducir(ctx);
                Symbol elemento = s.resuelto;
                actual = new IndiceAccess(actual, indice, Operandos.tipoDe(elemento),
                        elemento != null ? elemento.getTipoNombre() : null,
                        elemento != null ? elemento.getDimensiones() : 0);
            } else {
                List<MemoryAccess> argumentos = new ArrayList<>();
                for (Node argumento : s.argumentos) {
                    argumentos.add(((Expresion) argumento).traducir(ctx));
                }
                Symbol objetivo = s.resuelto;
                if (objetivo != null && objetivo.isNativa()) {
                    actual = traducirNativa(ctx, objetivo.getName(), argumentos);
                } else {
                    MemoryAccess receptor = receiverPara(metodoPendiente, objetivo, actual);
                    if (receptor != null) {
                        argumentos.add(0, receptor);
                    }
                    String callee = callePara(ctx, metodoPendiente, claseObjetivo, objetivo,
                            s.argumentos.size());
                    String ctypeRetorno = tipoRetornoDe(objetivo);
                    MemoryAccess destino = null;
                    if (ctypeRetorno != null) {
                        destino = Operandos.temporal(ctx, ctypeRetorno);
                    }
                    ctx.agregar(new Llamada3D(callee, argumentos, destino));
                    actual = destino;
                }
                metodoPendiente = null;
                claseObjetivo = null;
            }
        }
        return actual;
    }

    public MemoryAccess toLvalue(CodigoContexto ctx) {
        return traducir(ctx);
    }

    private MemoryAccess receiverPara(String metodoPendiente, Symbol objetivo, MemoryAccess actual) {
        if (metodoPendiente != null) {
            return actual;
        }
        if (objetivo != null && objetivo.getKind() == Symbol.Kind.METODO) {
            return new Literal3D("self", Type.CLASS);
        }
        return null;
    }

    private String callePara(CodigoContexto ctx, String metodoPendiente, String claseObjetivo,
            Symbol objetivo, int argCount) {
        List<List<Type>> firmas = objetivo != null
                ? objetivo.getFirmas() : java.util.Collections.emptyList();
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        if (metodoPendiente != null) {
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            return claseObjetivo + "_" + metodoPendiente + "_" + idx;
        }
        if (objetivo != null && objetivo.getKind() == Symbol.Kind.METODO) {
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            return ctx.currentClassName() + "_" + objetivo.getName() + "_" + idx;
        }
        return nombre;
    }

    private String tipoRetornoDe(Symbol objetivo) {
        if (objetivo == null) {
            return "int";
        }
        Type tipo = objetivo.getType();
        if (tipo == null || tipo == Type.VOID) {
            return null;
        }
        return Operandos.tipoRetornoC(tipo, objetivo.getTipoNombre());
    }

    private MemoryAccess traducirNativa(CodigoContexto ctx, String nombre, List<MemoryAccess> argumentos) {
        if (nombre.equals("leer") || nombre.equals("readln")) {
            MemoryAccess temp = Operandos.temporal(ctx, "int");
            ctx.agregar(new Leer3D(temp, "%d", true));
            return temp;
        }
        ctx.agregar(Impresion.print(ctx, argumentos));
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Symbol actual = ctx.resolver(nombre);
        if (actual == null) {
            ctx.registrarError(getLine(), getColumn(), "Variable no declarada: '" + nombre + "'");
            return null;
        }

        String metodoPendiente = null;
        Symbol contenedorPendiente = null;

        for (Sufijo s : sufijos) {
            switch (s.tipo) {
                case CAMPO: {
                    if (actual.getType() != Type.STRUCT && actual.getType() != Type.CLASS) {
                        ctx.registrarError(getLine(), getColumn(),
                            "No se puede acceder a '." + s.nombreCampo + "' sobre " + actual.getType());
                        return null;
                    }
                    Symbol tipoContenedor = ctx.resolver(actual.getTipoNombre());
                    if (tipoContenedor == null || tipoContenedor.getMiembros() == null) {
                        ctx.registrarError(getLine(), getColumn(),
                            "Tipo '" + actual.getTipoNombre() + "' sin miembros registrados");
                        return null;
                    }
                    Symbol miembro = tipoContenedor.getMiembros().resolve(s.nombreCampo);
                    s.resuelto = miembro;
                    s.claseObjetivo = tipoContenedor.getName();
                    if (miembro != null) {
                        actual = miembro;
                        metodoPendiente = null;
                    } else {
                        metodoPendiente = s.nombreCampo;
                        contenedorPendiente = tipoContenedor;
                    }
                    break;
                }
                case INDICE: {
                    if (!actual.isArray() && actual.getDimensiones() <= 0) {
                        ctx.registrarError(getLine(), getColumn(),
                            "No se puede indexar un valor que no es arreglo (" + actual.getType() + ")");
                        return null;
                    }
                    Type tipoIndice = ctx.evaluar(s.indice);
                    if (tipoIndice != null && tipoIndice != Type.INT) {
                        ctx.registrarError(getLine(), getColumn(),
                            "El índice de un arreglo debe ser entero, se dio " + tipoIndice);
                    }
                    int restantes = Math.max(0, actual.getDimensiones() - 1);
                    Symbol elemento = new Symbol(actual.getName(), actual.getType(), Symbol.Kind.VARIABLE, restantes > 0, false, false, 0);
                    elemento.setTipoNombre(actual.getTipoNombre());
                    elemento.setMiembros(actual.getMiembros());
                    elemento.setDimensiones(restantes);
                    s.resuelto = elemento;
                    actual = elemento;
                    break;
                }
                case LLAMADA: {
                    List<Type> tiposArgs = tiposDeArgumentos(ctx, s.argumentos);
                    if (metodoPendiente != null) {
                        Symbol metodo = contenedorPendiente.getMiembros().resolve(metodoPendiente);
                        if (metodo == null) {
                            ctx.registrarError(getLine(), getColumn(),
                                "No existe el método '" + metodoPendiente + "'");
                            return null;
                        }
                        if (!metodo.tieneFirmaCompatible(tiposArgs)) {
                            ctx.registrarError(getLine(), getColumn(),
                                "El método '" + metodoPendiente + "' no coincide con los argumentos dados");
                            return null;
                        }
                        s.resuelto = metodo;
                        s.claseObjetivo = contenedorPendiente.getName();
                        actual = metodo;
                        metodoPendiente = null;
                    } else {
                        Symbol objetivo = actual;
                        if (objetivo == null || (objetivo.getKind() != Symbol.Kind.FUNCION
                                && objetivo.getKind() != Symbol.Kind.METODO)) {
                            ctx.registrarError(getLine(), getColumn(),
                                "'" + nombre + "' no es una función conocida (¿falta un import?)");
                            return null;
                        }
                        s.resuelto = objetivo;
                        if (objetivo.isNativa()) {
                            return objetivo.getType();
                        }
                        if (objetivo.getKind() == Symbol.Kind.METODO) {
                            if (!objetivo.tieneFirmaCompatible(tiposArgs)) {
                                ctx.registrarError(getLine(), getColumn(),
                                    "El método '" + objetivo.getName()
                                    + "' no coincide con los argumentos dados");
                                return null;
                            }
                            actual = objetivo;
                            break;
                        }
                        if (tiposArgs.size() != objetivo.getTiposParametros().size()) {
                            ctx.registrarError(getLine(), getColumn(),
                                "La función '" + nombre + "' espera "
                                + objetivo.getTiposParametros().size() + " argumento(s) pero se dieron "
                                + tiposArgs.size());
                            return null;
                        }
                        for (int i = 0; i < tiposArgs.size(); i++) {
                            Type esperado = objetivo.getTiposParametros().get(i);
                            if (esperado == Type.ARRAY && s.argumentos.get(i) instanceof StructLiteralNode) {
                                continue;
                            }
                            if (!TypeCompat.esAsignable(esperado, tiposArgs.get(i))) {
                                ctx.registrarError(getLine(), getColumn(),
                                    "El argumento " + (i + 1) + " de '" + nombre + "' debe ser "
                                    + esperado + " pero se dio " + tiposArgs.get(i));
                                return null;
                            }
                        }
                        actual = objetivo;
                    }
                    break;
                }
            }
        }

        return actual.getType();
    }

    static List<Type> tiposDeArgumentos(ContextoSemantico ctx, List<Node> argumentos) {
        List<Type> tipos = new ArrayList<>();
        for (Node arg : argumentos) {
            tipos.add(tipoDeArgumento(ctx, arg));
        }
        return tipos;
    }

    private static Type tipoDeArgumento(ContextoSemantico ctx, Node arg) {
        Type tipo = ctx.evaluar(arg);
        if (tipo != Type.ARRAY && arg instanceof AccessNode acceso) {
            Symbol simbolo = resolverSimbolo(ctx, acceso);
            if (simbolo != null && simbolo.isArray()) {
                return Type.ARRAY;
            }
        }
        return tipo;
    }

    public static Symbol resolverSimbolo(ContextoSemantico ctx, AccessNode acceso) {
        Symbol actual = ctx.resolver(acceso.nombre);
        if (actual == null) {
            return null;
        }
        for (Sufijo s : acceso.sufijos) {
            if (s.tipo == Sufijo.Tipo.LLAMADA) {
                return null;
            }
            if (s.tipo == Sufijo.Tipo.CAMPO) {
                if (actual.getType() != Type.STRUCT && actual.getType() != Type.CLASS) {
                    return null;
                }
                Symbol contenedor = ctx.resolver(actual.getTipoNombre());
                if (contenedor == null || contenedor.getMiembros() == null) {
                    return null;
                }
                actual = contenedor.getMiembros().resolve(s.nombreCampo);
                if (actual == null) {
                    return null;
                }
            } else {
                if (!actual.isArray()) {
                    return null;
                }
                int restantes = Math.max(0, actual.getDimensiones() - 1);
                Symbol elemento = new Symbol(actual.getName(), actual.getType(), Symbol.Kind.VARIABLE,
                    restantes > 0, false, false, 0);
                elemento.setTipoNombre(actual.getTipoNombre());
                elemento.setMiembros(actual.getMiembros());
                elemento.setDimensiones(restantes);
                actual = elemento;
            }
        }
        return actual;
    }
}