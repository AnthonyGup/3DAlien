package cunoc.compi2.alien_code.ast.expr;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.semantic.ContextoSemantico;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ast.ASTVisitor;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.semantic.Symbol;

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
                    if (!actual.isArray()) {
                        ctx.registrarError(getLine(), getColumn(),
                            "No se puede indexar un valor que no es arreglo (" + actual.getType() + ")");
                        return null;
                    }
                    Type tipoIndice = ctx.evaluar(s.indice);
                    if (tipoIndice != null && tipoIndice != Type.INT) {
                        ctx.registrarError(getLine(), getColumn(),
                            "El índice de un arreglo debe ser entero, se dio " + tipoIndice);
                    }
                    Symbol elemento = new Symbol(actual.getName(), actual.getType(), Symbol.Kind.VARIABLE, false, false, false, 0);
                    elemento.setTipoNombre(actual.getTipoNombre());
                    elemento.setMiembros(actual.getMiembros());
                    actual = elemento;
                    break;
                }
                case LLAMADA: {
                    for (Node arg : s.argumentos) {
                        ctx.evaluar(arg);
                    }
                    if (metodoPendiente != null) {
                        Symbol metodo = contenedorPendiente.getMiembros().resolve(metodoPendiente);
                        if (metodo == null) {
                            ctx.registrarError(getLine(), getColumn(),
                                "No existe el método '" + metodoPendiente + "'");
                            return null;
                        }
                        actual = metodo;
                        metodoPendiente = null;
                    } else {
                        Symbol funcion = ctx.resolver(nombre);
                        if (funcion == null || funcion.getKind() != Symbol.Kind.FUNCION) {
                            ctx.registrarError(getLine(), getColumn(),
                                "'" + nombre + "' no es una función conocida (¿falta un import?)");
                            return null;
                        }
                        actual = funcion;
                    }
                    break;
                }
            }
        }

        return actual.getType();
    }
}