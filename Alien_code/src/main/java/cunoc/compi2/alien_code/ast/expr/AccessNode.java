package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.ArrayList;
import java.util.List;

public class AccessNode implements Node {
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

    public boolean conSufijos() {
        return !sufijos.isEmpty();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAccess(this);
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
    public String traducir(CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String actual = nombre;
        String pendingMethod = null;
        for (Sufijo s : sufijos) {
            if (s.tipo == Sufijo.Tipo.CAMPO) {
                if (isMethodName(gen, actual, s.nombreCampo)) {
                    pendingMethod = s.nombreCampo;
                } else {
                    String t = ctx.nuevoTemporal();
                    ctx.emitir(".", actual, s.nombreCampo, t);
                    registrarCampo(gen, t, actual, s.nombreCampo);
                    actual = t;
                }
            } else if (s.tipo == Sufijo.Tipo.INDICE) {
                String idx = s.indice.traducir(ctx);
                String t = ctx.nuevoTemporal();
                ctx.emitir("[]", actual, idx, t);
                registrarElemento(gen, t, actual);
                actual = t;
            } else {
                Symbol target = resolveCallTarget(gen, pendingMethod, actual);
                StringBuilder args = new StringBuilder();
                for (Node arg : s.argumentos) {
                    if (args.length() > 0) args.append(", ");
                    args.append(arg.traducir(ctx));
                }
                if (target != null && target.isNativa()) {
                    actual = translateNativeCall(gen, target, args.toString(), ctx);
                } else {
                    String t = ctx.nuevoTemporal();
                    String self = selfPara(gen, pendingMethod, target, actual);
                    String finalArgs = self != null
                            ? self + (args.length() > 0 ? ", " + args : "")
                            : args.toString();
                    String callee = callePara(gen, pendingMethod, target, actual, s.argumentos.size());
                    gen.recordTemporal(t, target != null ? target.getType() : null,
                            false, 0, target != null ? target.getTipoNombre() : null);
                    ctx.emitir("call", callee, finalArgs, t);
                    actual = t;
                }
                pendingMethod = null;
            }
        }
        return actual;
    }

    public String toLvalue(CodigoContexto ctx) {
        if (sufijos.isEmpty()) {
            return nombre;
        }
        String actual = nombre;
        for (int i = 0; i < sufijos.size() - 1; i++) {
            actual = applyReadSuffix(actual, sufijos.get(i), ctx);
        }
        Sufijo last = sufijos.get(sufijos.size() - 1);
        if (last.tipo == Sufijo.Tipo.CAMPO) {
            return actual + "." + last.nombreCampo;
        }
        if (last.tipo == Sufijo.Tipo.INDICE) {
            return actual + "[" + last.indice.traducir(ctx) + "]";
        }
        return actual;
    }

    private String applyReadSuffix(String actual, Sufijo s, CodigoContexto ctx) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        if (s.tipo == Sufijo.Tipo.CAMPO) {
            String t = ctx.nuevoTemporal();
            ctx.emitir(".", actual, s.nombreCampo, t);
            registrarCampo(gen, t, actual, s.nombreCampo);
            return t;
        }
        if (s.tipo == Sufijo.Tipo.INDICE) {
            String idx = s.indice.traducir(ctx);
            String t = ctx.nuevoTemporal();
            ctx.emitir("[]", actual, idx, t);
            registrarElemento(gen, t, actual);
            return t;
        }
        return actual;
    }

    private boolean isMethodName(IntermediateCodeGenerator gen, String actual, String campo) {
        IntermediateCodeGenerator.ValueInfo info = gen.describeValue(actual);
        if (info == null || (info.type != Type.STRUCT && info.type != Type.CLASS)) {
            return false;
        }
        if (info.tipoNombre == null) {
            return false;
        }
        Symbol contenedor = gen.resolveForTranslation(info.tipoNombre);
        if (contenedor == null || contenedor.getMiembros() == null) {
            return false;
        }
        Symbol miembro = contenedor.getMiembros().resolve(campo);
        return miembro != null && miembro.getKind() == Symbol.Kind.METODO;
    }

    private Symbol resolveCallTarget(IntermediateCodeGenerator gen, String pendingMethod, String actual) {
        if (pendingMethod != null) {
            IntermediateCodeGenerator.ValueInfo info = gen.describeValue(actual);
            if (info == null || info.tipoNombre == null) {
                return null;
            }
            Symbol contenedor = gen.resolveForTranslation(info.tipoNombre);
            if (contenedor == null || contenedor.getMiembros() == null) {
                return null;
            }
            return contenedor.getMiembros().resolve(pendingMethod);
        }
        return gen.resolveForTranslation(actual);
    }

    private String selfPara(IntermediateCodeGenerator gen, String pendingMethod, Symbol target, String actual) {
        if (pendingMethod != null) {
            return actual;
        }
        if (target != null && target.getKind() == Symbol.Kind.METODO) {
            return "self";
        }
        return null;
    }

    private String callePara(IntermediateCodeGenerator gen, String pendingMethod, Symbol target,
            String actual, int argCount) {
        List<List<Type>> firmas = target != null ? target.getFirmas() : java.util.Collections.emptyList();
        if (pendingMethod != null) {
            IntermediateCodeGenerator.ValueInfo info = gen.describeValue(actual);
            String nombreClase = info != null ? info.tipoNombre : "";
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            return nombreClase + "_" + pendingMethod + "_" + idx;
        }
        if (target != null && target.getKind() == Symbol.Kind.METODO) {
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            return gen.currentClassName() + "_" + target.getName() + "_" + idx;
        }
        return nombre;
    }

    private String translateNativeCall(IntermediateCodeGenerator gen, Symbol target, String args,
            CodigoContexto ctx) {
        if (target.getName().equals("leer") || target.getName().equals("readln")) {
            String t = ctx.nuevoTemporal();
            gen.recordTemporalType(t, Type.STRING);
            ctx.emitir("read", null, null, t);
            return t;
        }
        ctx.emitir("print", args, null, null);
        return null;
    }

    private void registrarCampo(IntermediateCodeGenerator gen, String t, String contenedor, String campo) {
        IntermediateCodeGenerator.ValueInfo info = gen.describeValue(contenedor);
        if (info == null || (info.type != Type.STRUCT && info.type != Type.CLASS)) {
            return;
        }
        if (info.tipoNombre == null) {
            return;
        }
        Symbol tipo = gen.resolveForTranslation(info.tipoNombre);
        if (tipo == null || tipo.getMiembros() == null) {
            return;
        }
        Symbol miembro = tipo.getMiembros().resolve(campo);
        if (miembro == null) {
            return;
        }
        gen.recordTemporal(t, miembro.getType(), miembro.isArray(),
                miembro.getDimensiones(), miembro.getTipoNombre());
    }

    private void registrarElemento(IntermediateCodeGenerator gen, String t, String contenedor) {
        IntermediateCodeGenerator.ValueInfo info = gen.describeValue(contenedor);
        if (info == null) {
            return;
        }
        int restantes = Math.max(0, info.dimensiones - 1);
        gen.recordTemporal(t, info.type, restantes > 0, restantes, info.tipoNombre);
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