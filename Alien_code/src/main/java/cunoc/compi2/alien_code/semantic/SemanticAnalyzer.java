package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.decl.ConstructorDeclNode;
import cunoc.compi2.alien_code.ast.decl.FunctionDeclNode;
import cunoc.compi2.alien_code.ast.decl.MethodDeclNode;
import cunoc.compi2.alien_code.ast.decl.StructDeclNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.errors.ErrorListener;

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
        paseB(programas);
    }

    public void paseA(List<ProgramNode> programas) {
        for (ProgramNode programa : programas) {
            for (Node declaracion : programa.declarations) {
                registrarFirma(declaracion);
            }
        }
    }

    public void paseB(List<ProgramNode> programas) {
        for (ProgramNode programa : programas) {
            programa.analizar(contexto);
        }
    }

    private void registrarFirma(Node nodo) {
        if (nodo instanceof StructDeclNode struct) {
            contexto.definir(new Symbol(struct.nombre, Type.STRUCT, Symbol.Kind.ESTRUCTURA, false, false, false, struct.campos.size()));
            contexto.entrarAmbito();
            for (VariableDeclNode campo : struct.campos) {
                contexto.definir(new Symbol(campo.nombre, campo.tipo, Symbol.Kind.VARIABLE, false, false, true, 0));
            }
            contexto.salirAmbito();
        } else if (nodo instanceof FunctionDeclNode funcion) {
            Type retorno = funcion.tipoRetorno == null ? Type.VOID : funcion.tipoRetorno;
            contexto.definir(new Symbol(funcion.nombre, retorno, Symbol.Kind.FUNCION, false, false, false, funcion.parametros.size()));
        } else if (nodo instanceof ClassDeclNode clase) {
            contexto.definir(new Symbol(clase.nombre, Type.CLASS, Symbol.Kind.CLASE, false, false, false, clase.atributos.size()));
            contexto.entrarAmbito();
            for (VariableDeclNode atributo : clase.atributos) {
                contexto.definir(new Symbol(atributo.nombre, atributo.tipo, Symbol.Kind.VARIABLE, false, false, true, 0));
            }
            for (MethodDeclNode metodo : clase.metodos) {
                Type retorno = metodo.tipoRetorno == null ? Type.VOID : metodo.tipoRetorno;
                contexto.definir(new Symbol(metodo.nombre, retorno, Symbol.Kind.METODO, false, false, false, metodo.parametros.size()));
            }
            for (ConstructorDeclNode constructor : clase.constructores) {
                contexto.definir(new Symbol(clase.nombre, Type.VOID, Symbol.Kind.CONSTRUCTOR, false, false, false, constructor.parametros.size()));
            }
            contexto.salirAmbito();
        }
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}