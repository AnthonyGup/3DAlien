package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.decl.FunctionDeclNode;
import cunoc.compi2.alien_code.ast.decl.MethodDeclNode;
import cunoc.compi2.alien_code.ast.decl.ParameterNode;
import cunoc.compi2.alien_code.ast.decl.StructDeclNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.ast.stmt.BlockNode;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.errors.ErrorListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SemanticAnalyzerTest {

    @Test
    void paseARegistraFirmasDeStructFuncionYClase() {
        StructDeclNode persona = new StructDeclNode("Persona",
                List.of(new VariableDeclNode("edad", Type.INT, null, null, 1, 1)), 1, 1);

        FunctionDeclNode calcularPoder = new FunctionDeclNode("calcularPoder",
                List.of(new ParameterNode(Type.INT, "fuerza", false, 2, 1)), Type.INT,
                new BlockNode(2, 10), 2, 1);

        MethodDeclNode mover = new MethodDeclNode("mover", List.of(), Type.VOID, new BlockNode(5, 5), 5, 1);
        ClassDeclNode robot = new ClassDeclNode("Robot", List.of(), List.of(), List.of(mover), 4, 1);

        ProgramNode programa = programaY(List.of(persona, calcularPoder, robot));

        SemanticAnalyzer analizador = new SemanticAnalyzer(new ErrorListener());
        analizador.analizar(List.of(programa));
        SymbolTable tabla = analizador.getSymbolTable();

        Symbol simboloPersona = tabla.resolve("Persona");
        assertNotNull(simboloPersona);
        assertEquals(Type.STRUCT, simboloPersona.getType());
        assertEquals(Symbol.Kind.ESTRUCTURA, simboloPersona.getKind());
        assertEquals(1, simboloPersona.getSize());

        Symbol simboloPoder = tabla.resolve("calcularPoder");
        assertNotNull(simboloPoder);
        assertEquals(Type.INT, simboloPoder.getType());
        assertEquals(Symbol.Kind.FUNCION, simboloPoder.getKind());
        assertEquals(1, simboloPoder.getSize());

        Symbol simboloRobot = tabla.resolve("Robot");
        assertNotNull(simboloRobot);
        assertEquals(Type.CLASS, simboloRobot.getType());
        assertEquals(Symbol.Kind.CLASE, simboloRobot.getKind());

        assertNull(tabla.resolve("mover"));
    }

    @Test
    void simboloInexistenteDevuelveNulo() {
        ProgramNode programa = programaY(List.of());
        SemanticAnalyzer analizador = new SemanticAnalyzer(new ErrorListener());
        analizador.analizar(List.of(programa));
        assertNull(analizador.getSymbolTable().resolve("inexistente"));
    }

    @Test
    void analizarDosProgramasComparteLaTablaGlobal() {
        FunctionDeclNode funcionY = new FunctionDeclNode("yo", List.of(), Type.VOID, new BlockNode(1, 1), 1, 1);
        ClassDeclNode claseZ = new ClassDeclNode("Zeta", List.of(), List.of(), List.of(), 1, 1);

        ProgramNode programaY = programaY("Y?", List.of(funcionY));
        ProgramNode programaZ = programaY("Zetariano", List.of(claseZ));

        SemanticAnalyzer analizador = new SemanticAnalyzer(new ErrorListener());
        analizador.analizar(List.of(programaY, programaZ));

        SymbolTable tabla = analizador.getSymbolTable();
        assertEquals(Symbol.Kind.FUNCION, tabla.resolve("yo").getKind());
        assertEquals(Symbol.Kind.CLASE, tabla.resolve("Zeta").getKind());
        assertEquals(2, tabla.listarSimbolos().size());
    }

    @Test
    void sinErroresElListenerQuedaVacio() {
        ProgramNode programa = programaY(List.of());
        ErrorListener errores = new ErrorListener();
        SemanticAnalyzer analizador = new SemanticAnalyzer(errores);
        analizador.analizar(List.of(programa));
        assertFalse(errores.hasErrors());
    }

    private ProgramNode programaY(List<Node> declaraciones) {
        return programaY("Y?", declaraciones);
    }

    private ProgramNode programaY(String lenguaje, List<Node> declaraciones) {
        ProgramNode programa = new ProgramNode(1, 1);
        programa.sourceLanguage = lenguaje;
        programa.declarations = declaraciones;
        return programa;
    }
}