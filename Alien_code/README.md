# 3DAlien Compiler

Compilador multi-lenguaje para el curso de Compiladores 2.

## Lenguajes soportados

| Lenguaje | Descripción |
|---|---|
| **Y?** | Lenguaje con sintaxis en español |
| **Zetariano** | Lenguaje con sintaxis pseudo-latina |
| **Pig Latin** | Lenguaje con sintaxis en inglés (estilo Pig Latin) |

Los tres lenguajes comparten los mismos conceptos (variables, funciones, structs/clases, ciclos, condicionales) pero con sintaxis diferente. Cada lenguaje define su gramática y su ASTBuilder (que genera nodos del AST unificado); el **AST, el análisis semántico en dos pases y el backend** son comunes (la traducción de tipo se apoya en `Type.fromPigLatin`/`fromYLang`/`fromZetariano`). La semántica valida el `.pig` junto con sus imports `.y`/`.z` en dos pases (Pase A: declaraciones/firmas; Pase B: verificación), lo que permite resolver llamadas a funciones importadas sin depender del orden de los archivos.

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
  Cuartetas tipadas  →  List<c3d.cuartetas.Cuarteta>
        │
        ▼
  CCodeGenerator (toCCode: C3D = C)  →  Código C compilable
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
│   ├── pigLatin/    # grammar, astbuilder
│   ├── ylang/       # grammar, astbuilder
│   ├── zetariano/   # grammar, astbuilder
│   ├── ast/         # AST unificado: program, expr, stmt, decl + Type
│   ├── semantic/    # SemanticAnalyzer (Pase A/B), ContextoSemantico, Symbol/SymbolTable/Scope
│   ├── ir/          # CodigoContexto, IntermediateCodeGenerator, Operandos, Impresion
│   ├── c3d/         # Cuartetas tipadas + accesos (cuartetas/, access/, TiposC)
│   ├── codegen/     # CCodeGenerator (Generador de código C)
│   ├── errors/      # Manejo de errores
│   ├── ui/          # Swing: editor, resaltado, consola, pipeline
│   └── Alien_code.java
```

## Compilar y ejecutar

```bash
# Generar fuentes ANTLR y compilar
mvn clean compile

# Ejecutar la aplicación gráfica (aquí se prueba todo)
mvn exec:java
```

## Equipo

- Anthony - Compiladores 2, CUNOC
