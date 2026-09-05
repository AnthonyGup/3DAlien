# 3DAlien Compiler

Compilador multi-lenguaje para el curso de Compiladores 2.

## Lenguajes soportados

| Lenguaje | Descripción |
|---|---|
| **Y?** | Lenguaje con sintaxis en español |
| **Zetariano** | Lenguaje con sintaxis pseudo-latina |
| **Pig Latin** | Lenguaje con sintaxis en inglés (estilo Pig Latin) |

Los tres lenguajes comparten los mismos conceptos (variables, funciones, structs/clases, ciclos, condicionales) pero con sintaxis diferente. El compilador genera un AST unificado que permite reutilizar la semántica y generación de código para los tres.

## Stack tecnológico

- **Java 17+**
- **ANTLR4** - Análisis léxico y sintáctico
- **JavaFX** - Interfaz gráfica
- **Maven** - Build tool
- **C** - Código objetivo (generación de código C)

## Pipeline de compilación

```
Código fuente (.y / .z / .pig)
        │
        ▼
  ANTLR Lexer/Parser  →  ParseTree
        │
        ▼
  ASTBuilder (puente)  →  AST propio
        │
        ▼
  Analizador Semántico  →  Tabla de símbolos, decoración de tipos
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
├── docs/
│   └── diagrama_clases.png
├── src/main/antlr4/cunoc/compi2/alien_code/grammar/
│   ├── YLang.g4
│   ├── Zetariano.g4
│   └── PigLatin.g4
├── src/main/java/cunoc/compi2/alien_code/
│   ├── ast/           # Nodos del AST propio + ASTVisitor
│   ├── astbuilder/    # Puentes ANTLR → AST
│   ├── semantic/      # Tabla de símbolos, analizador semántico
│   ├── ir/            # Cuartetas, generador de código intermedio
│   ├── c3d/           # Generador C3D
│   ├── codegen/       # Generador de código C
│   ├── errors/        # Manejo de errores
│   ├── ui/            # JavaFX: editor, resaltado, consola
│   └── Alien_code.java
└── src/test/java/cunoc/compi2/alien_code/
```

## Compilar y ejecutar

```bash
# Generar fuentes ANTLR
mvn generate-sources

# Compilar
mvn compile

# Ejecutar
mvn exec:java

# Ejecutar interfaz gráfica (JavaFX)
mvn javafx:run
```

## Equipo

- Anthony - Compiladores 2, CUNOC
