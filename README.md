# 3DAlien

Compilador de tres lenguajes inventados (Pig Latin, Y?, Zetariano) a C.  
Proyecto de Compiladores 2, CUNOC.

---

## Lenguajes

| Archivo | Lenguaje | Qué hace |
|---------|----------|----------|
| `.pig` | Pig Latin | Programa principal (main = `MAIOR> ... FINIS ;`) |
| `.y` | Y? | Estructuras y funciones reutilizables (indentación) |
| `.z` | Zetariano | Clases estilo Java (una por archivo) |

El `.pig` importa `.y` y `.z` con `import ruta.con.puntos`.

---

## Requisitos

- Java 21
- Maven 3.9+
- (Opcional) gcc / clang / cl.exe para compilar el `.c` generado

---

## Compilar y ejecutar

```bash
cd Alien_code
mvn -B clean compile
mvn exec:java
```

Se abre la ventana de la GUI.

---

## Cómo usar la GUI

1. **Archivo → Abrir carpeta de proyecto** — elige la carpeta que tenga tus `.pig`, `.y`, `.z`.
2. El árbol de la izquierda muestra los archivos. Doble clic abre en el editor.
3. **Botón "Compilar main (.pig)"** — busca el `.pig` con `MAIOR>`, resuelve imports y compila todo.
4. El log de abajo muestra errores, warnings y éxito.
5. **Reportes** → Ver errores / Tabla de símbolos / Cuartetas / C3D / Código C.
6. En "Ver código C" hay un botón **"Compilar C"** que usa gcc/clang/cl.exe y deja el ejecutable en `carpeta_proyecto/build/`.

---

## Estructura del proyecto

```
3DAlien/
├── MANUAL_TECNICO.md
├── README.md
└── Alien_code/
    ├── pom.xml
    └── src/
        ├── main/antlr4/...      # 3 gramáticas ANTLR
        └── main/java/...        # código Java
            ├── ast/             # 34 nodos únicos
            ├── semantic/        # semántica 2 pases
            ├── pigLatin/astbuilder/
            ├── ylang/astbuilder/
            ├── zetariano/astbuilder/
            ├── ir/              # generador de cuartetas
            ├── c3d/             # accesos + 14 cuartetas
            ├── codegen/         # CCodeGenerator
            └── ui/              # GUI Swing
```

---

## Pipeline

1. **ANTLR** genera lexers/parsers de las 3 gramáticas.
2. **Builders** convierten ParseTree → AST único (`ast.*`).
3. **SemanticAnalyzer** en 2 pases:
   - Pase A: declara structs, funciones, clases (firmas).
   - Pase B: verifica cuerpos (resuelve imports).
4. **IntermediateCodeGenerator** emite **cuartetas tipadas** (`c3d.cuartetas.*`).
5. **CCodeGenerator** render `toCCode` → código C compilable.

---

## Build JAR

```bash
cd Alien_code
mvn -B clean package
```

Genera `target/Alien_code-1.0-SNAPSHOT.jar` (fat JAR con dependencias).  
Ejecutar: `java -jar target/Alien_code-1.0-SNAPSHOT.jar`

---

## Notas

- No hay tests automáticos. Se prueba desde la GUI.
- Validación del `.c` generado: solo visual (no hay gcc en la máquina de desarrollo).
- No se edita `target/generated-sources/antlr4`.
- Convenciones: Java 21, nodos POJO, sin comentarios en código, API semántica en español.