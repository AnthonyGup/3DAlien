# 3DAlien Compiler

Compilador multi-lenguaje para el curso de Compiladores 2.

## Lenguajes soportados

| Lenguaje | Descripción |
|---|---|
| **Y?** | Lenguaje con sintaxis en español |
| **Zetariano** | Lenguaje con sintaxis pseudo-latina |
| **Pig Latin** | Lenguaje con sintaxis en inglés (estilo Pig Latin) |

Los tres lenguajes comparten los mismos conceptos (variables, funciones, structs/clases, ciclos, condicionales) pero con sintaxis diferente. Cada lenguaje define su gramática, su ASTBuilder (que genera nodos del AST unificado) y su Vocabulary; el **AST, el análisis semántico en dos pases y el backend** son comunes. La semántica valida el `.pig` junto con sus imports `.y`/`.z` en dos pases (Pase A: declaraciones/firmas; Pase B: verificación), lo que permite resolver llamadas a funciones importadas sin depender del orden de los archivos.

## Stack tecnológico

- **Java 21**
- **ANTLR4** - Análisis léxico y sintáctico
- **Swing + RSyntaxTextArea** - Interfaz gráfica con resaltado por lenguaje
- **Maven** - Build tool
- **C** - Código objetivo (generación de código C)

## Pipeline de compilación

```
Código fuente (.y / .z / .pig)
        │
        ▼
  ANTLR Lexer/Parser  →  ParseTree          (por lenguaje)
        │
        ▼
  ASTBuilder → nodos del AST unificado      (por lenguaje, emiten ast.*)
        │
        ▼
  SemanticAnalyzer → Pase A (firmas) + Pase B (verificación)   (.pig + imports .y/.z)
        │
        ▼
  Generador de Cuartetas  →  List<Cuarteta>
        │
        ▼
  Generador C3D  →  Instrucciones C3D
        │
        ▼
  Generador de código C  →  Código C compilable
```

## Estructura del proyecto

```
Alien_code/
├── pom.xml
├── src/main/antlr4/cunoc/compi2/alien_code/
│   ├── pigLatin/grammar/PigLatin.g4
│   ├── ylang/grammar/YLangLexer.g4 + YLangParser.g4
│   └── zetariano/grammar/Zetariano.g4
├── src/main/java/cunoc/compi2/alien_code/
│   ├── pigLatin/    # grammar, astbuilder, semantic (Vocabulary)
│   ├── ylang/       # grammar, astbuilder, semantic (Vocabulary)
│   ├── zetariano/   # grammar, astbuilder, semantic (Vocabulary)
│   ├── ast/         # AST unificado: program, expr, stmt, decl + Type
│   ├── semantic/    # SemanticAnalyzer (Pase A/B), ContextoSemantico, Symbol/SymbolTable/Scope
│   ├── ir/          # CodigoContexto, Cuartetas, IR
│   ├── c3d/         # Generador C3D
│   ├── codegen/     # Generador de código C
│   ├── errors/      # Manejo de errores
│   ├── ui/          # Swing: editor, resaltado, consola, pipeline
│   └── Alien_code.java
└── src/test/java/cunoc/compi2/alien_code/
    ├── ylang/grammar/YLangGrammarTest.java
    ├── zetariano/grammar/ZetarianoGrammarTest.java
    └── semantic/SemanticAnalyzerTest.java
```

## Compilar y ejecutar

```bash
# Generar fuentes ANTLR y compilar
mvn clean compile

# Ejecutar pruebas
mvn test

# Ejecutar la aplicación gráfica
mvn exec:java
```

## Equipo

- Anthony - Compiladores 2, CUNOC
