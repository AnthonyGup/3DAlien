package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.decl.ConstructorDeclNode;
import cunoc.compi2.alien_code.ast.decl.FunctionDeclNode;
import cunoc.compi2.alien_code.ast.decl.MethodDeclNode;
import cunoc.compi2.alien_code.ast.decl.ParameterNode;
import cunoc.compi2.alien_code.ast.decl.StructDeclNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.errors.ErrorListener;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable;
    private final ContextoSemanticoImpl contexto;

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
        }
    }

    public void paseA(List<ProgramNode> programas) {
        for (ProgramNode programa : programas) {
            for (Node declaracion : programa.declarations) {
                if (declaracion instanceof StructDeclNode struct) {
                    registrarEstructura(struct, contexto);
                } else if (declaracion instanceof FunctionDeclNode funcion) {
                    registrarFuncion(funcion, contexto);
                } else if (declaracion instanceof ClassDeclNode clase) {
                    registrarClase(clase, contexto);
                }
            }
        }
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
            programa.analizar(contexto);
        }
    }

    private void registrarClase(ClassDeclNode nodo, ContextoSemantico ctx) {
        ctx.entrarAmbito();

        for (VariableDeclNode attr : nodo.atributos) {
            Symbol s = Symbol.variable(attr.nombre, attr.tipo, attr.tipoElemento,
                    attr.tipoNombre, attr.dimensiones, false, true,
                    attr.getLine(), attr.getColumn());
            if (!ctx.definir(s)) {
                ctx.registrarError(attr.getLine(), attr.getColumn(),
                    "Atributo duplicado '" + attr.nombre + "' en la clase '" + nodo.nombre + "'");
            }
        }

        for (MethodDeclNode m : nodo.metodos) {
            List<Type> tipos = new ArrayList<>();
            for (ParameterNode p : m.parametros) { tipos.add(p.tipo); }

            Symbol existente = ctx.ambitoActual().resolveLocal(m.nombre);
            if (existente == null) {
                Symbol ms = new Symbol(m.nombre, m.tipoRetorno, Symbol.Kind.METODO, false, false, false, 0);
                if (m.tipoRetorno == Type.STRUCT || m.tipoRetorno == Type.CLASS) {
                    ms.setTipoNombre(m.tipoRetornoNombre);
                }
                ms.setLinea(m.getLine());
                ms.setColumna(m.getColumn());
                ms.agregarFirma(tipos);
                ctx.definir(ms);
            } else {
                existente.agregarFirma(tipos);
            }
        }

        Scope scopeMiembros = ctx.ambitoActual();
        ctx.salirAmbito();

        Symbol claseSimbolo = new Symbol(nodo.nombre, Type.CLASS, Symbol.Kind.CLASE, false, false, false,
            nodo.atributos.size());
        claseSimbolo.setLinea(nodo.getLine());
        claseSimbolo.setColumna(nodo.getColumn());
        claseSimbolo.setMiembros(scopeMiembros);
        for (ConstructorDeclNode c : nodo.constructores) {
            List<Type> tipos = new ArrayList<>();
            for (ParameterNode p : c.parametros) tipos.add(p.tipo);
            claseSimbolo.agregarFirmaConstructor(tipos);
        }

        if (!ctx.definir(claseSimbolo)) {
            ctx.registrarError(nodo.getLine(), nodo.getColumn(),
                "La clase '" + nodo.nombre + "' ya fue declarada");
        }
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}