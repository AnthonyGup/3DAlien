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
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.Impresion;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.semantic.POO;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.TypeCompat;

import java.util.ArrayList;
import java.util.List;

public class AccessNode extends Expresion {
    public String nombre;
    public List<Sufijo> sufijos;
    private Symbol baseResuelto;
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
        Symbol baseSymbol = baseResuelto != null ? baseResuelto : ctx.resolverSimbolo(nombre);
        boolean esThis = nombre.equals("this");
        MemoryAccess actual = esThis
                ? new Literal3D("self", Type.CLASS)
                : Operandos.nombre(ctx, nombre, baseSymbol);
        String claseBase = esThis && ctx instanceof IntermediateCodeGenerator g
                ? g.currentClassName() : actual.getTipoNombre();
        String metodoPendiente = null;
        String claseObjetivo = null;
        for (Sufijo s : sufijos) {
            if (s.tipo == Sufijo.Tipo.CAMPO) {
                if (s.resuelto != null && s.resuelto.getKind() == Symbol.Kind.METODO) {
                    metodoPendiente = s.nombreCampo;
                    claseObjetivo = s.claseObjetivo;
                } else {
                    Symbol miembro = s.resuelto;
                    CampoAccess campo = new CampoAccess(actual, s.nombreCampo, Operandos.tipoDe(miembro),
                            miembro != null ? miembro.getTipoNombre() : null,
                            miembro != null ? miembro.getDimensiones() : 0);
                    if (ctx instanceof IntermediateCodeGenerator gen2) {
                        campo.setOffset(gen2.offsetCampo(claseBase, s.nombreCampo));
                    }
                    actual = campo;
                    claseBase = campo.getTipoNombre();
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
                    String tipoEstaticoRecv = esThis ? claseBase : actual.getTipoNombre();
                    MemoryAccess receptor = receiverPara(metodoPendiente, objetivo, actual);
                    String ctypeRetorno = tipoRetornoDe(objetivo);
                    MemoryAccess destino = null;
                    if (ctypeRetorno != null) {
                        destino = Operandos.temporal(ctx, ctypeRetorno);
                    }
                    LlamadaVirtual3D virtual = intentarVirtual(ctx, objetivo, tipoEstaticoRecv,
                            receptor, argumentos, destino, s.argumentos.size());
                    if (virtual != null) {
                        ctx.agregar(virtual);
                    } else {
                        if (receptor != null) {
                            argumentos.add(0, receptor);
                        }
                        String callee = callePara(ctx, metodoPendiente, claseObjetivo, objetivo,
                                s.argumentos.size());
                        ctx.agregar(new Llamada3D(callee, argumentos, destino));
                    }
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

    private LlamadaVirtual3D intentarVirtual(CodigoContexto ctx, Symbol objetivo,
            String tipoEstatico, MemoryAccess receptor, List<MemoryAccess> argumentos,
            MemoryAccess destino, int argCount) {
        if (!(ctx instanceof IntermediateCodeGenerator gen) || gen.getLayouts() == null) {
            return null;
        }
        if (receptor == null || objetivo == null || objetivo.getKind() != Symbol.Kind.METODO) {
            return null;
        }
        if (objetivo.getVisibilidad() == cunoc.compi2.alien_code.ast.decl.Visibilidad.PRIVATE) {
            return null;
        }
        String claseEstatica = tipoEstatico;
        if ((claseEstatica == null || !gen.getLayouts().contiene(claseEstatica))
                && receptor instanceof Literal3D lit && "self".equals(lit.getTexto())) {
            claseEstatica = gen.currentClassName();
        }
        if (claseEstatica == null || !gen.getLayouts().contiene(claseEstatica)) {
            return null;
        }
        cunoc.compi2.alien_code.ir.ClaseLayout layout = gen.getLayouts().layoutDe(claseEstatica);
        int slot = layout.slotDe(objetivo.getName());
        if (slot < 0) {
            return null;
        }
        List<cunoc.compi2.alien_code.ir.ClaseLayout.ImplMetodo> candidatos =
                gen.getLayouts().candidatos(claseEstatica, objetivo.getName());
        if (candidatos.size() <= 1) {
            return null;
        }
        return new LlamadaVirtual3D(receptor, slot, objetivo.getName(), argumentos, destino, candidatos);
    }

    private String callePara(CodigoContexto ctx, String metodoPendiente, String claseObjetivo,
            Symbol objetivo, int argCount) {
        List<List<Type>> firmas = objetivo != null
                ? objetivo.getFirmas() : java.util.Collections.emptyList();
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        if (metodoPendiente != null) {
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            String clase = objetivo != null && objetivo.getClaseDuena() != null
                    ? objetivo.getClaseDuena() : claseObjetivo;
            return clase + "_" + metodoPendiente + "_" + idx;
        }
        if (objetivo != null && objetivo.getKind() == Symbol.Kind.METODO) {
            int idx = gen.overloadIndexByArgCount(firmas, argCount);
            String clase = objetivo.getClaseDuena() != null
                    ? objetivo.getClaseDuena() : ctx.currentClassName();
            return clase + "_" + objetivo.getName() + "_" + idx;
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
            MemoryAccess temp = Operandos.temporal(ctx, "double");
            ctx.agregar(new Leer3D(temp, "%lf", true));
            return temp;
        }
        ctx.agregar(Impresion.print(ctx, argumentos));
        return null;
    }
    @Override
    public Type analizar(ContextoSemantico ctx) {
        Symbol actual;
        if (nombre.equals("this")) {
            if (!ctx.enRutina() || ctx.claseActual() == null) {
                ctx.registrarError(getLine(), getColumn(),
                    "'this' solo puede usarse dentro de una clase");
                return null;
            }
            actual = ctx.claseActual();
        } else {
            actual = ctx.resolver(nombre);
            if (actual == null && ctx.claseActual() != null) {
                Symbol clase = ctx.claseActual();
                Symbol miembro = POO.buscarAtributoIncluyendoPrivado(clase, nombre);
                if (miembro != null) {
                    if (POO.puedeAcceder(miembro, clase)) {
                        actual = miembro;
                    } else {
                        String duena = miembro.getClaseDuena() != null ? miembro.getClaseDuena() : "?";
                        ctx.registrarError(getLine(), getColumn(),
                            "'" + nombre + "' es privado en '" + duena + "'");
                        return null;
                    }
                }
            }
            if (actual == null && ctx.claseActual() != null && esLlamadaDirecta()) {
                Symbol clase = ctx.claseActual();
                Symbol metodo = POO.buscarMetodoIncluyendoPrivado(clase, nombre);
                if (metodo != null) {
                    if (POO.puedeAcceder(metodo, clase)) {
                        actual = metodo;
                    } else {
                        String duena = metodo.getClaseDuena() != null ? metodo.getClaseDuena() : "?";
                        ctx.registrarError(getLine(), getColumn(),
                            "'" + nombre + "' es privado en '" + duena + "'");
                        return null;
                    }
                }
            }
            if (actual == null) {
                ctx.registrarError(getLine(), getColumn(), "Variable no declarada: '" + nombre + "'");
                return null;
            }
        }
        baseResuelto = actual;

        String metodoPendiente = null;
        Symbol contenedorPendiente = null;

        for (Sufijo s : sufijos) {
            switch (s.tipo) {
                case CAMPO: {
                    if (actual.getType() != Type.STRUCT && actual.getType() != Type.CLASS
                            && actual.getKind() != Symbol.Kind.CLASE
                            && actual.getKind() != Symbol.Kind.ESTRUCTURA) {
                        ctx.registrarError(getLine(), getColumn(),
                            "No se puede acceder a '." + s.nombreCampo + "' sobre " + actual.getType());
                        return null;
                    }
                    Symbol tipoContenedor = contenedorDe(ctx, actual);
                    if (tipoContenedor == null || tipoContenedor.getMiembros() == null) {
                        ctx.registrarError(getLine(), getColumn(),
                            "Tipo '" + nombreTipo(actual) + "' sin miembros registrados");
                        return null;
                    }
                    Symbol miembro = POO.buscarAtributo(tipoContenedor, s.nombreCampo);
                    if (miembro == null) {
                        miembro = tipoContenedor.getMiembros().resolveLocal(s.nombreCampo);
                    }
                    if (miembro != null && miembro.getKind() == Symbol.Kind.METODO) {
                        s.resuelto = miembro;
                        s.claseObjetivo = tipoContenedor.getName();
                        metodoPendiente = s.nombreCampo;
                        contenedorPendiente = tipoContenedor;
                    } else {
                        s.resuelto = miembro;
                        s.claseObjetivo = tipoContenedor.getName();
                        if (miembro != null) {
                            verificarAcceso(ctx, miembro);
                            actual = miembro;
                            metodoPendiente = null;
                        } else {
                            Symbol metodoHeredado = POO.buscarMetodo(tipoContenedor, s.nombreCampo);
                            if (metodoHeredado != null) {
                                s.resuelto = metodoHeredado;
                            }
                            metodoPendiente = s.nombreCampo;
                            contenedorPendiente = tipoContenedor;
                        }
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
                        Symbol metodo = POO.buscarMetodo(contenedorPendiente, metodoPendiente);
                        if (metodo == null) {
                            Symbol privado = POO.buscarMetodoIncluyendoPrivado(contenedorPendiente, metodoPendiente);
                            if (privado != null && !POO.puedeAcceder(privado, ctx.claseActual())) {
                                String duena = privado.getClaseDuena() != null ? privado.getClaseDuena() : "?";
                                ctx.registrarError(getLine(), getColumn(),
                                    "'" + metodoPendiente + "' es privado en '" + duena + "'");
                                return null;
                            }
                            if (privado != null) {
                                metodo = privado;
                            }
                        }
                        if (metodo == null) {
                            ctx.registrarError(getLine(), getColumn(),
                                    "'" + contenedorPendiente.getName() + "' no tiene un método '"
                                    + metodoPendiente + "'");
                            return null;
                        }
                        if (!metodo.tieneFirmaCompatible(tiposArgs)) {
                            ctx.registrarError(getLine(), getColumn(),
                                "El método '" + metodoPendiente + "' no coincide con los argumentos dados");
                            return null;
                        }
                        verificarAcceso(ctx, metodo);
                        s.resuelto = metodo;
                        s.claseObjetivo = contenedorPendiente.getName();
                        actual = metodo;
                        metodoPendiente = null;
                    } else if (actual != null && (actual.getKind() == Symbol.Kind.CLASE
                            || actual.getKind() == Symbol.Kind.ESTRUCTURA)) {
                        Symbol metodo = POO.buscarMetodo(actual, nombre);
                        if (metodo == null) {
                            ctx.registrarError(getLine(), getColumn(),
                                "'" + actual.getName() + "' no tiene un método '" + nombre + "'");
                            return null;
                        }
                        s.resuelto = metodo;
                        if (!metodo.tieneFirmaCompatible(tiposArgs)) {
                            ctx.registrarError(getLine(), getColumn(),
                                "El método '" + metodo.getName() + "' no coincide con los argumentos dados");
                            return null;
                        }
                        verificarAcceso(ctx, metodo);
                        actual = metodo;
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

    private boolean esLlamadaDirecta() {
        return !sufijos.isEmpty() && sufijos.get(0).tipo == Sufijo.Tipo.LLAMADA;
    }

    private Symbol contenedorDe(ContextoSemantico ctx, Symbol actual) {
        if (actual.getKind() == Symbol.Kind.CLASE || actual.getKind() == Symbol.Kind.ESTRUCTURA) {
            return actual;
        }
        Symbol porNombre = ctx.resolver(actual.getTipoNombre());
        if (porNombre != null) {
            return porNombre;
        }
        return POO.buscarClase(ctx, actual.getTipoNombre());
    }

    private String nombreTipo(Symbol actual) {
        if (actual.getTipoNombre() != null) return actual.getTipoNombre();
        return actual.getName();
    }

    private void verificarAcceso(ContextoSemantico ctx, Symbol miembro) {
        Symbol contexto = ctx.claseActual();
        if (POO.puedeAcceder(miembro, contexto)) {
            return;
        }
        String duena = miembro.getClaseDuena() != null ? miembro.getClaseDuena() : "?";
        if (miembro.getVisibilidad() == cunoc.compi2.alien_code.ast.decl.Visibilidad.PRIVATE) {
            ctx.registrarError(getLine(), getColumn(),
                "'" + miembro.getName() + "' es privado en '" + duena + "'");
        } else {
            ctx.registrarError(getLine(), getColumn(),
                "'" + miembro.getName() + "' es protegido en '" + duena
                + "' y solo es accesible desde sus subclases");
        }
    }

    public static List<Type> tiposDeArgumentos(ContextoSemantico ctx, List<Node> argumentos) {
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
        Symbol actual = acceso.nombre.equals("this") ? ctx.claseActual() : ctx.resolver(acceso.nombre);
        if (actual == null) {
            return null;
        }
        for (Sufijo s : acceso.sufijos) {
            if (s.tipo == Sufijo.Tipo.LLAMADA) {
                return null;
            }
            if (s.tipo == Sufijo.Tipo.CAMPO) {
                Symbol contenedor;
                if (actual.getKind() == Symbol.Kind.CLASE || actual.getKind() == Symbol.Kind.ESTRUCTURA) {
                    contenedor = actual;
                } else {
                    if (actual.getType() != Type.STRUCT && actual.getType() != Type.CLASS) {
                        return null;
                    }
                    contenedor = ctx.resolver(actual.getTipoNombre());
                }
                if (contenedor == null || contenedor.getMiembros() == null) {
                    return null;
                }
                actual = POO.buscarAtributo(contenedor, s.nombreCampo);
                if (actual == null) {
                    actual = contenedor.getMiembros().resolveLocal(s.nombreCampo);
                }
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