package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.decl.ConstructorDeclNode;
import cunoc.compi2.alien_code.ast.decl.FunctionDeclNode;
import cunoc.compi2.alien_code.ast.decl.MethodDeclNode;
import cunoc.compi2.alien_code.ast.decl.ParameterNode;
import cunoc.compi2.alien_code.ast.decl.StructDeclNode;
import cunoc.compi2.alien_code.ast.decl.Visibilidad;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.errors.ErrorListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable;
    private final ContextoSemanticoImpl contexto;
    private final Map<String, ClassDeclNode> clasesPorNombre = new LinkedHashMap<>();
    private final Map<ClassDeclNode, Symbol> simbolosClase = new HashMap<>();
    private final Map<ClassDeclNode, String> archivoDeClase = new HashMap<>();

    public SemanticAnalyzer(ErrorListener errorListener) {
        this.symbolTable = new SymbolTable();
        this.contexto = new ContextoSemanticoImpl(symbolTable, errorListener);
    }

    public void analizar(List<ProgramNode> programas) {
        paseA(programas);
        registrarNativas(programas);
        paseB(programas);
    }

    private void registrarNativas(List<ProgramNode> programas) {
        boolean conY = programas.stream().anyMatch(p -> "Y?".equals(p.sourceLanguage));
        boolean conZ = programas.stream().anyMatch(p -> "Zetariano".equals(p.sourceLanguage));
        if (conY) {
            Symbol imprimir = new Symbol("imprimir", Type.VOID, Symbol.Kind.FUNCION,
                false, false, false, 0);
            imprimir.setNativa(true);
            contexto.definir(imprimir);
            Symbol leer = new Symbol("leer", null, Symbol.Kind.FUNCION,
                false, false, false, 0);
            leer.setNativa(true);
            contexto.definir(leer);
        }
        if (conZ) {
            Symbol println = new Symbol("println", Type.VOID, Symbol.Kind.FUNCION,
                false, false, false, 0);
            println.setNativa(true);
            contexto.definir(println);
            Symbol print = new Symbol("print", Type.VOID, Symbol.Kind.FUNCION,
                false, false, false, 0);
            print.setNativa(true);
            contexto.definir(print);
        }
    }

    public void paseA(List<ProgramNode> programas) {
        List<ClassDeclNode> clases = new ArrayList<>();
        for (ProgramNode programa : programas) {
            contexto.ingresarPrograma(programa.archivo);
            for (Node declaracion : programa.declarations) {
                if (declaracion instanceof StructDeclNode struct) {
                    registrarEstructura(struct, contexto);
                } else if (declaracion instanceof FunctionDeclNode funcion) {
                    registrarFuncion(funcion, contexto);
                } else if (declaracion instanceof ClassDeclNode clase) {
                    clases.add(clase);
                    archivoDeClase.put(clase, programa.archivo);
                }
            }
        }
        contexto.ingresarPrograma(null);
        procesarClases(clases);
    }

    private void procesarClases(List<ClassDeclNode> clases) {
        // 1. Registrar todas las clases (shells) sin mirar miembros.
        for (ClassDeclNode clase : clases) {
            contexto.ingresarPrograma(archivoDeClase.get(clase));
            Symbol shell = new Symbol(clase.nombre, Type.CLASS, Symbol.Kind.CLASE,
                false, false, false, clase.atributos.size());
            shell.setLinea(clase.getLine());
            shell.setColumna(clase.getColumn());
            shell.setNombreSuperclase(clase.superclase);
            shell.setVisibilidad(clase.visibilidad);
            if (!contexto.definir(shell)) {
                contexto.registrarError(clase.getLine(), clase.getColumn(),
                    "La clase '" + clase.nombre + "' ya fue declarada");
            } else {
                clasesPorNombre.put(clase.nombre, clase);
                simbolosClase.put(clase, shell);
            }
        }
        // 2. Resolver superclases y detectar ciclos (E01, E02).
        for (ClassDeclNode clase : clases) {
            Symbol shell = simbolosClase.get(clase);
            if (shell == null || clase.superclase == null) continue;
            contexto.ingresarPrograma(archivoDeClase.get(clase));
            Symbol padre = POO.buscarClase(contexto, clase.superclase);
            if (padre == null) {
                contexto.registrarError(clase.getLine(), clase.getColumn(),
                    "La clase '" + clase.superclase + "' no existe (superclase de '" + clase.nombre + "')");
                continue;
            }
            shell.setSuperclaseResuelta(padre);
        }
        for (ClassDeclNode clase : clases) {
            contexto.ingresarPrograma(archivoDeClase.get(clase));
            detectarCicloClase(clase);
        }
        contexto.ingresarPrograma(null);

        // 3. Orden topológico: padres antes que hijos.
        List<ClassDeclNode> ordenadas = ordenTopologico(clases);

        // 4. Registrar miembros y validar @Override / duplicados.
        for (ClassDeclNode clase : ordenadas) {
            contexto.ingresarPrograma(archivoDeClase.get(clase));
            registrarMiembrosClase(clase);
        }

        // 5. Validar constructores (super).
        for (ClassDeclNode clase : ordenadas) {
            contexto.ingresarPrograma(archivoDeClase.get(clase));
            validarConstructores(clase);
        }
        contexto.ingresarPrograma(null);
    }

    private void detectarCicloClase(ClassDeclNode clase) {
        Symbol shell = simbolosClase.get(clase);
        if (shell == null) return;
        List<String> camino = new ArrayList<>();
        Symbol actual = shell;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (camino.contains(actual.getName())) {
                camino.add(actual.getName());
                contexto.registrarError(clase.getLine(), clase.getColumn(),
                    "Herencia cíclica: " + String.join(" -> ", camino));
                shell.setSuperclaseResuelta(null);
                return;
            }
            camino.add(actual.getName());
            actual = actual.getSuperclaseResuelta();
        }
    }

    private List<ClassDeclNode> ordenTopologico(List<ClassDeclNode> clases) {
        List<ClassDeclNode> resultado = new ArrayList<>();
        List<String> puestas = new ArrayList<>();
        for (ClassDeclNode clase : clases) {
            agregarEnOrden(clase, resultado, puestas, 0);
        }
        return resultado;
    }

    private void agregarEnOrden(ClassDeclNode clase, List<ClassDeclNode> resultado,
            List<String> puestas, int profundidad) {
        if (clase == null || puestas.contains(clase.nombre) || profundidad > 1000) return;
        Symbol shell = simbolosClase.get(clase);
        Symbol padre = shell != null ? shell.getSuperclaseResuelta() : null;
        if (padre != null) {
            ClassDeclNode padreNodo = clasesPorNombre.get(padre.getName());
            if (padreNodo != null) agregarEnOrden(padreNodo, resultado, puestas, profundidad + 1);
        }
        puestas.add(clase.nombre);
        resultado.add(clase);
    }


    private void registrarEstructura(StructDeclNode nodo, ContextoSemantico ctx) {
        ctx.entrarAmbito();
        for (VariableDeclNode campo : nodo.campos) {
            Symbol campoSimbolo = Symbol.variable(campo.nombre, campo.tipo, campo.tipoElemento,
                    campo.tipoNombre, campo.dimensiones, false, true,
                    campo.getLine(), campo.getColumn());
            if (!ctx.definir(campoSimbolo)) {
                ctx.registrarError(campo.getLine(), campo.getColumn(),
                    "Campo duplicado '" + campo.nombre + "' en la estructura '" + nodo.nombre + "'");
            }
        }
        Scope scopeMiembros = ctx.ambitoActual();
        ctx.salirAmbito();

        Symbol simbolo = new Symbol(nodo.nombre, Type.STRUCT, Symbol.Kind.ESTRUCTURA,
            false, false, false, nodo.campos.size());
        simbolo.setMiembros(scopeMiembros);
        simbolo.setLinea(nodo.getLine());
        simbolo.setColumna(nodo.getColumn());
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(nodo.getLine(), nodo.getColumn(),
                "La estructura '" + nodo.nombre + "' ya fue declarada");
        }
    }

    private void registrarFuncion(FunctionDeclNode nodo, ContextoSemantico ctx) {
        Symbol simbolo = new Symbol(nodo.nombre, nodo.tipoRetorno, Symbol.Kind.FUNCION,
            false, false, false, nodo.parametros.size());
        if (nodo.tipoRetorno == Type.STRUCT || nodo.tipoRetorno == Type.CLASS) {
            simbolo.setTipoNombre(nodo.tipoRetornoNombre);
        }
        simbolo.setLinea(nodo.getLine());
        simbolo.setColumna(nodo.getColumn());
        for (ParameterNode p : nodo.parametros) {
            simbolo.getTiposParametros().add(p.tipo);
        }
        if (!ctx.definir(simbolo)) {
            ctx.registrarError(nodo.getLine(), nodo.getColumn(),
                "La función '" + nodo.nombre + "' ya fue declarada");
        }
    }

    public void paseB(List<ProgramNode> programas) {
        for (ProgramNode programa : programas) {
            contexto.ingresarPrograma(programa.archivo);
            programa.analizar(contexto);
        }
        contexto.ingresarPrograma(null);
    }

    private void registrarMiembrosClase(ClassDeclNode nodo) {
        Symbol claseSimbolo = simbolosClase.get(nodo);
        if (claseSimbolo == null) return;

        contexto.entrarAmbito();
        ContextoSemantico ctx = contexto;

        for (VariableDeclNode attr : nodo.atributos) {
            Symbol s = Symbol.variable(attr.nombre, attr.tipo, attr.tipoElemento,
                    attr.tipoNombre, attr.dimensiones, false, true,
                    attr.getLine(), attr.getColumn());
            s.setVisibilidad(attr.visibilidad);
            s.setClaseDuena(nodo.nombre);
            Symbol heredado = buscarAtributoEnAncestros(claseSimbolo, attr.nombre);
            if (heredado != null) {
                ctx.registrarError(attr.getLine(), attr.getColumn(),
                    "El atributo '" + attr.nombre + "' ya está declarado en la superclase '"
                    + heredado.getClaseDuena() + "'");
            }
            if (!ctx.definir(s)) {
                ctx.registrarError(attr.getLine(), attr.getColumn(),
                    "Atributo duplicado '" + attr.nombre + "' en la clase '" + nodo.nombre + "'");
            }
        }

        for (MethodDeclNode m : nodo.metodos) {
            List<Type> tipos = new ArrayList<>();
            for (ParameterNode p : m.parametros) { tipos.add(p.tipo); }

            Symbol propio = ctx.ambitoActual().resolveLocal(m.nombre);
            if (propio != null && propio.getKind() == Symbol.Kind.METODO) {
                if (firmaRepetida(propio, tipos)) {
                    ctx.registrarError(m.getLine(), m.getColumn(),
                        "El método '" + m.nombre + "' ya fue declarado con esa firma en la clase '"
                        + nodo.nombre + "'");
                    continue;
                }
                propio.agregarFirma(tipos);
                propio.getTiposParametros().addAll(tipos);
                validarOverride(nodo, claseSimbolo, m, propio, tipos);
                continue;
            }
            Symbol ms = new Symbol(m.nombre, m.tipoRetorno, Symbol.Kind.METODO, false, false, false, 0);
            if (m.tipoRetorno == Type.STRUCT || m.tipoRetorno == Type.CLASS) {
                ms.setTipoNombre(m.tipoRetornoNombre);
            }
            ms.setLinea(m.getLine());
            ms.setColumna(m.getColumn());
            ms.setVisibilidad(m.visibilidad);
            ms.setClaseDuena(nodo.nombre);
            ms.setTieneOverride(m.tieneOverride);
            ms.agregarFirma(tipos);
            ms.getTiposParametros().addAll(tipos);

            validarOverride(nodo, claseSimbolo, m, ms, tipos);
            ctx.definir(ms);
        }

        Scope scopeMiembros = ctx.ambitoActual();
        ctx.salirAmbito();
        claseSimbolo.setMiembros(scopeMiembros);
    }

    private Symbol buscarAtributoEnAncestros(Symbol clase, String nombre) {
        Symbol padre = clase.getSuperclaseResuelta();
        int guarda = 0;
        while (padre != null && guarda++ < 1000) {
            Symbol m = padre.getMiembros() != null
                ? padre.getMiembros().resolveLocal(nombre) : null;
            if (m != null && m.getKind() == Symbol.Kind.VARIABLE) {
                return m;
            }
            padre = padre.getSuperclaseResuelta();
        }
        return null;
    }

    private void validarOverride(ClassDeclNode nodo, Symbol clase, MethodDeclNode m,
            Symbol ms, List<Type> tipos) {
        Symbol enPadre = buscarMetodoMismaFirma(clase, m.nombre, tipos);
        if (m.tieneOverride) {
            Symbol ancestro = buscarMetodoConNombre(clase, m.nombre);
            if (ancestro == null) {
                contexto.registrarError(m.getLine(), m.getColumn(),
                    "@Override en '" + m.nombre + "', pero ninguna superclase define ese método");
                return;
            }
            if (enPadre == null) {
                contexto.registrarError(m.getLine(), m.getColumn(),
                    "'" + m.nombre + "' no tiene la misma firma que '" + ancestro.getClaseDuena()
                    + "." + m.nombre + "'; tipos o retorno incompatibles");
                return;
            }
            if (!retornoCompatible(enPadre, ms)) {
                contexto.registrarError(m.getLine(), m.getColumn(),
                    "'" + m.nombre + "' no tiene la misma firma que '" + enPadre.getClaseDuena()
                    + "." + m.nombre + "'; tipos o retorno incompatibles");
            }
            if (visibilidadMenor(m.visibilidad, enPadre.getVisibilidad())) {
                contexto.registrarError(m.getLine(), m.getColumn(),
                    "'" + m.nombre + "' no puede ser menos visible que '" + enPadre.getClaseDuena()
                    + "." + m.nombre + "'");
            }
        } else {
            if (enPadre != null) {
                contexto.registrarError(m.getLine(), m.getColumn(),
                    "El método '" + m.nombre + "' sobrescribe a '" + enPadre.getClaseDuena()
                    + "." + m.nombre + "' y requiere @Override");
            }
        }
    }

    private boolean firmaRepetida(Symbol simbolo, List<Type> tipos) {
        for (List<Type> f : simbolo.getFirmas()) {
            if (mismosTipos(f, tipos)) return true;
        }
        return false;
    }

    private Symbol buscarMetodoConNombre(Symbol clase, String nombre) {
        Symbol padre = clase.getSuperclaseResuelta();
        int guarda = 0;
        while (padre != null && guarda++ < 1000) {
            Symbol m = padre.getMiembros() != null
                ? padre.getMiembros().resolveLocal(nombre) : null;
            if (m != null && m.getKind() == Symbol.Kind.METODO
                    && m.getVisibilidad() != Visibilidad.PRIVATE) {
                return m;
            }
            padre = padre.getSuperclaseResuelta();
        }
        return null;
    }

    private Symbol buscarMetodoMismaFirma(Symbol clase, String nombre, List<Type> tipos) {
        Symbol padre = clase.getSuperclaseResuelta();
        int guarda = 0;
        while (padre != null && guarda++ < 1000) {
            Symbol m = padre.getMiembros() != null
                ? padre.getMiembros().resolveLocal(nombre) : null;
            if (m != null && m.getKind() == Symbol.Kind.METODO
                    && m.getVisibilidad() != Visibilidad.PRIVATE) {
                for (List<Type> f : m.getFirmas()) {
                    if (mismosTipos(f, tipos)) return m;
                }
            }
            padre = padre.getSuperclaseResuelta();
        }
        return null;
    }

    private boolean mismosTipos(List<Type> a, List<Type> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) return false;
        }
        return true;
    }

    private boolean retornoCompatible(Symbol padre, Symbol hijo) {
        return padre.getType() == hijo.getType();
    }

    private boolean visibilidadMenor(Visibilidad hija, Visibilidad padre) {
        return rango(hija) < rango(padre);
    }

    private int rango(Visibilidad v) {
        if (v == Visibilidad.PRIVATE) return 0;
        if (v == Visibilidad.PROTECTED) return 1;
        return 2;
    }

    private void validarConstructores(ClassDeclNode nodo) {
        Symbol claseSimbolo = simbolosClase.get(nodo);
        if (claseSimbolo == null) return;

        List<ConstructorDeclNode> constructores = nodo.constructores;
        if (constructores.isEmpty()) {
            Symbol implicito = new Symbol("<init>", Type.VOID, Symbol.Kind.CONSTRUCTOR,
                false, false, false, 0);
            implicito.setClaseDuena(nodo.nombre);
            implicito.setVisibilidad(nodo.visibilidad);
            claseSimbolo.agregarConstructor(implicito);
            claseSimbolo.agregarFirmaConstructor(new ArrayList<>());
            if (claseSimbolo.getSuperclaseResuelta() != null
                    && !tieneConstructorVacio(claseSimbolo.getSuperclaseResuelta())
                    && claseSimbolo.getSuperclaseResuelta().getSuperclaseResuelta() != null) {
                contexto.registrarError(nodo.getLine(), nodo.getColumn(),
                    "'" + claseSimbolo.getSuperclaseResuelta().getName()
                    + "' no tiene constructor sin parámetros: llame a super(...)");
            }
            return;
        }

        for (ConstructorDeclNode c : constructores) {
            List<Type> tipos = new ArrayList<>();
            for (ParameterNode p : c.parametros) tipos.add(p.tipo);
            Symbol ctor = new Symbol("<init>", Type.VOID, Symbol.Kind.CONSTRUCTOR,
                false, false, false, 0);
            ctor.setClaseDuena(nodo.nombre);
            ctor.setVisibilidad(c.visibilidad);
            ctor.getTiposParametros().addAll(tipos);
            ctor.setLinea(c.getLine());
            ctor.setColumna(c.getColumn());
            claseSimbolo.agregarConstructor(ctor);
            claseSimbolo.agregarFirmaConstructor(tipos);
        }
    }

    private boolean tieneConstructorVacio(Symbol clase) {
        if (clase == null) return false;
        for (Symbol ctor : clase.getConstructores()) {
            if (ctor.getTiposParametros().isEmpty()) return true;
        }
        // Clase con constructor implícito (sin constructores declarados) => vacío
        return clase.getConstructores().isEmpty();
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}