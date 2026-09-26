# Manual Técnico 3DAlien

**Proyecto:** Compilador 3DAlien  
**Curso:** Compiladores 2, CUNOC  
**Fecha:** Septiembre 2026

---

## 1. Qué es esto

3DAlien compila tres lenguajes inventados (`.pig`, `.y`, `.z`) a código C. El flujo es:  
lexer/parser ANTLR → AST único → semántica en dos pases → cuartetas tipadas → C.

El `.pig` es el programa principal (tiene `MAIOR>` que es el main). Los `.y` y `.z` son librerías que importa el `.pig`. Los tres comparten el mismo AST y la misma semántica.

---

## 2. Lenguajes

| Extensión | Lenguaje | Qué define |
|-----------|----------|------------|
| `.pig` | Pig Latin | Programa principal, main = `MAIOR> ... FINIS ;` |
| `.y` | Y? | Estructuras y funciones (importables), indentación tipo Python |
| `.z` | Zetariano | Clases estilo Java, una clase por archivo |

---

## 3. Arquitectura

```
.pig / .y / .z  →  ANTLR (3 gramáticas)  →  3 Builders  →  AST único (ast.*)
                                                            ↓
                                                  SemanticAnalyzer (2 pases)
                                                    ↓
                                              Cuartetas tipadas
                                                    ↓
                                            CCodeGenerator → .c
```

- **Pase A** (solo firmas): recorre todos los archivos (.pig + imports) y registra en la tabla de símbolos: structs, funciones, clases (con sus campos/métodos/constructores).
- **Pase B** (verificación): analiza los cuerpos. Como Pase A ya registró todo, los imports se resuelven sin importar el orden.

---

## 4. Build y ejecución

```bash
cd Alien_code
mvn -B clean compile     # genera ANTLR + compila
mvn exec:java            # abre la GUI
```

Java 21, ANTLR 4.13.1, RSyntaxTextArea 3.3.4. No hay tests automáticos, se prueba desde la GUI.

---

## 5. Estructura del proyecto

```
3DAlien/
├── MANUAL_TECNICO.md
├── README.md
└── Alien_code/
    ├── pom.xml
    └── src/
        ├── main/antlr4/.../pigLatin/grammar/PigLatin.g4
        ├── main/antlr4/.../ylang/grammar/YLangLexer.g4 + YLangParser.g4
        ├── main/antlr4/.../zetariano/grammar/Zetariano.g4
        └── main/java/cunoc/compi2/alien_code/
            ├── Alien_code.java              # main → VentanaPrincipal
            ├── ast/                         # 34 nodos únicos
            ├── semantic/                    # semántica compartida
            ├── pigLatin/astbuilder/...      # PigLatinASTBuilder
            ├── ylang/astbuilder/...         # YLangASTBuilder
            ├── zetariano/astbuilder/...     # ZetarianoASTBuilder
            ├── ir/                          # CodigoContexto, IntermediateCodeGenerator
            ├── c3d/                         # accesos + 14 cuartetas
            ├── codegen/                     # CCodeGenerator
            ├── errors/                      # ErrorListener, CompilerError
            └── ui/                          # VentanaPrincipal, menús, tabs, log, etc.
```

---

## 6. AST principal (34 nodos)

Paquete `cunoc.compi2.alien_code.ast.*`. Interfaz raíz `Node` + `Expresion` y `Sentencia`.  
Nodos clave:
- **program:** `ProgramNode`, `ImportNode`
- **decl:** `StructDeclNode`, `FunctionDeclNode`, `ClassDeclNode`, `ConstructorDeclNode`, `MethodDeclNode`, `ParameterNode`
- **expr:** `AccessNode`, `BinaryOpNode`, `UnaryOpNode`, `LiteralNode`, `NewObjectNode`, `StructLiteralNode`, `ConditionalNode`, `NewArrayNode`
- **stmt:** `VariableDeclNode`, `ArrayDeclNode`, `AssignmentNode`, `IncrementNode`, `DecrementNode`, `BlockNode`, `IfNode`, `ElseIfNode`, `WhileNode`, `DoWhileNode`, `ForNode`, `BreakNode`, `ContinueNode`, `PrintNode`, `ReadNode`, `ReturnNode`, `SwitchNode`, `CaseNode`

Todos implementan `analizar(ContextoSemantico)` y `traducir(CodigoContexto)`.

---

## 7. Semántica

### 7.1 Symbol
`Symbol` guarda: nombre, tipo, kind (VARIABLE, FUNCION, ESTRUCTURA, CLASE, METODO, CONSTRUCTOR), si es array/campo/parámetro, `tipoNombre` (para STRUCT/CLASS), `miembros` (Scope con campos/métodos), firmas para sobrecargas, `isNativa` para built-ins.

### 7.2 Scope y SymbolTable
`Scope` = mapa ordenado + padre. `SymbolTable` = pila de scopes + registro permanente. `enterScopeWithin(padre)` permite que el cuerpo de un método cuelgue del scope de miembros de su clase (así `nombre = x` resuelve el atributo sin `this.`).

### 7.3 ContextoSemantico
Interfaz + `ContextoSemanticoImpl` sobre `SymbolTable`. Maneja ámbito, pila de retornos, pila de clase actual, ciclos, switches, errores.

### 7.4 SemanticAnalyzer
- `paseA(listaProgramas)`: registra structs, funciones, clases (atributos + métodos con sobrecarga + constructores).
- `registrarNativas`: `imprimir/leer` (Y?) y `println` (Z).
- `paseB(listaProgramas)`: llama `programa.analizar(ctx)` en cada uno.

### 7.4 TypeCompat
Compatibilidad central. Ensanchamiento `INT→FLOAT`. `+` en `STRING` concatena. `null` asignable a STRUCT/CLASS. Comparación `==/!=` con `NULL`. Relacionales solo numéricos.

---

## 8. Builders

- **PigLatinASTBuilder**: termina. `construir(String)` → `ProgramNode`. Mapea todo: imports, declaraciones, control, E/S, expresiones, accesos encadenados.
- **YLangASTBuilder**: implementado. `structura`→`StructDeclNode`, `funcion`→`FunctionDeclNode` con `ParameterNode` (refs con `[]`/`{}`). Indentación vía lexer (INDENT/DEDENT).
- **ZetarianoASTBuilder**: implementado. `public class`→`ClassDeclNode`. Arreglos `int[]`→`VariableDeclNode(tipo=ARRAY, tipoElemento, dimensiones)`. Desugar `+=` etc. Cuerpos sin llaves envueltos en `BlockNode`.

Los tres usan `sourceLanguage` ("Pig Latin", "Y?", "Zetariano").

---

## 9. Backend (IR, C3D, C)

- `ir.CodigoContexto`: contrato para emitir (temporales, etiquetas, ciclos, clases, cuartetas).
- `ir.IntermediateCodeGenerator`: implementa el contexto, genera `List<Cuarteta>`. Fábricas: `Operandos.temporal()`, `Operandos.nombre()`, `Impresion.print()`.
- `c3d.access`: `MemoryAccess` + 6 subclases (`NameAccess`, `TemporalAccess`, `CampoAccess`, `IndiceAccess`, `Literal3D`, `LabelAccess`).
- `c3d.cuartetas`: 14 clases (`Asignacion3D`, `Operacion3D`, `Condicional3D`, `Goto3D`, `Etiqueta3D`, `Llamada3D`, `Retornar3D`, `Halt3D`, `InicioFuncion3D`, `FinFuncion3D`, `Imprimir3D`, `Leer3D`, `DeclararArreglo3D`, `Cuarteta` base).
- `codegen.CCodeGenerator`: render a C. Helpers `conc/strd/strn` para string + número.

El `toCCode` de cada cuarteta es **a la vez C3D y C final**. No hay generador C3D intermedio.

---

## 10. GUI

- `VentanaPrincipal`: JFrame principal. `MainPanel` con árbol de proyecto (izq), tabs de editor (centro), log (abajo).
- `VentanaMenuBar`: menús Archivo/Reportes/Ayuda + botones "Compilar main (.pig)" y "Limpiar log".
- `VerificadorSintactico`: chequeo sintáctico del archivo activo antes de semántica.
- `PanelArbolProyecto`: árbol de archivos, doble clic abre en editor.
- `PanelLog`: consola con colores (info, éxito, error, pendiente).
- Reportes: errores, símbolos, cuartetas, C3D, código C (ventana con botón "Compilar C" que usa gcc/clang/cl.exe y guarda en `carpeta_proyecto/build/`).

---

## 11. Cosas que faltan / limitaciones

- Arreglos declarados como campo (`VariableDeclNode`): no se valida tamaño al asignar literal.
- Llamadas con literal `{...}` a métodos/constructores no validan elementos.
- `ReturnNode` fuera de función = sin error.
- Compatibilidad STRUCT/CLASS compara solo `Type` (no el nombre).
- Built-ins: si el usuario declara `imprimir`, la suya gana en silencio.
- No hay tests automáticos; todo se prueba manual en la GUI.
- No hay herencia ni encapsulamiento en Zetariano.

---

## 12. Convenciones de código

- Java 21. Nodos con campos públicos (POJO). Línea/columna base 1.
- `traducir` y `analizar` en cada nodo. Sin patrón Visitor.
- Sin comentarios en código fuente.
- Nombres de clases en inglés (`CodeGenerator`, `SymbolTable`), API semántica en español (`CodigoContexto`, `traducir`, `agregar`).
- Archivos generados (ANTLR) en `target/generated-sources/antlr4` — **no se tocan**.
- Build: `mvn -B clean compile`. Fat JAR con `maven-shade-plugin`.
- No se usa gcc en la máquina; validación de `.c` solo visual.

---

## 13. Cómo agregar features

1. Gramática ANTLR (`.g4`) → `mvn clean compile` regenera lexer/parser.
2. Nodo en `ast.*` si no existe (campo público, constructor, `getLine/getColumn`, `analizar`, `traducir`).
3. Mapeo en el builder correspondiente (`visitXxx`).
4. Si toca tipos: `Type`, `TypeCompat`, `Symbol`.
5. Si toca semántica: `ContextoSemantico`, `SemanticAnalyzer` (Pase A/B).
6. Si emite C: `CCodeGenerator` + cuartetas/accesos si hace falta.
7. Probar desde la GUI: abrir carpeta → botón "Compilar main (.pig)" → ver log/reportes.