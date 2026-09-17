# Manual Técnico del Proyecto 3DAlien

**Proyecto:** Compilador multilingüe 3DAlien
**Asignatura:** Compiladores 2, CUNOC
**Versión del código:** 1.0-SNAPSHOT
**Fecha:** Septiembre de 2026

---

## 0. Propósito y audiencia de este manual

Este manual es la **fuente de verdad del estado actual** del compilador 3DAlien. Está
escrito para que una IA generadora de **planes por fases** pueda producir planes
ejecutables sin necesidad de leer el código completo: describe la arquitectura, las
especificaciones de los tres lenguajes, la referencia exacta de cada clase (paquetes,
campos, constructores), lo que ya está hecho, lo que falta y las restricciones de diseño
deben respetarse.

Reglas para quien genere planes:

1. **Un plan por fase** debe terminar compilando (`mvn -B clean compile`). La verificación
   funcional se hace **desde el frontend** (la GUI), no con pruebas automatizadas: el
   proyecto **no tiene tests**.
2. Todo plan debe enumerar, para cada archivo a tocar: ruta, qué cambiar y por qué.
3. Todo plan debe respetar las convenciones de la sección 17 (sin comentarios en
   código, idioma de los nombres, columnas base 1, etc.).
4. El plan no debe reinventar clases ya existentes: usarlas tal cual (ver secciones 9–16).
5. Los pendientes prioritarios y su orden de dependencia están en la sección 19.

---

## 1. Visión general

3DAlien es un compilador de tres lenguajes de programación ficticios:

| Lenguaje | Extensión | Rol |
|---|---|---|
| **Y?** | `.y` | Define **estructuras y funciones** reutilizables (importadas por Pig Latin). No hay variables globales. Sintaxis por **indentación** (estilo Python). |
| **Zetariano** | `.z` | Define **clases/objetos** (importadas por Pig Latin). Sintaxis estilo **Java** (`{ }`, `;`). Un archivo = una clase `public`. |
| **Pig Latin** | `.pig` | Lenguaje **principal**: contiene la sección `MAIOR>` (el "main"). NO declara estructuras ni funciones: las importa de `.y`/`.z`. Sintaxis Pig Latin (`MAIOR>`, `FINIS;`, `VARIABILES>`). |

Los tres son **case sensitive**. Los tres producen nodos del **mismo AST** (`ast.*`) y son
validados por un **único** análisis semántico en **dos pases**. El backend (cuartetas →
C3D → C) es un único pipeline compartido.

---

## 2. Arquitectura (corregida)

La corrección `Correccion_AST_Unificado_y_Merge.md` (pendiente en
`C:\Users\Anthony\Downloads\Correccion_AST_Unificado_y_Merge.md`) eliminó dos defectos del
diseño original:

1. Triplicación del AST y de la semántica (una copia por idioma) → **una sola copia**.
2. El "Merge" ambiguo y el orden de validación que impedía resolver imports → un análisis
   **en dos pases** (declarar todo primero, verificar todo después).

Principio central: **se separa lo que de verdad es distinto por idioma de lo que no lo es**:

| Distinto por idioma (se mantiene separado) | Compartido (una sola implementación) |
|---|---|
| Gramática ANTLR (`.g4`) | Nodos del AST (`ast.*`) |
| `ASTBuilder` (traduce el ParseTree de ANTLR a nodos `ast.*`) | Análisis semántico (`semantic.*`) |
| `Vocabulary` trivial (traduce nombres de tipo) | Generación de cuartetas / C3D / C |

### Diagrama de flujo

```
                 .pig          .y           .z
                   |             |             |
                   v             v             v
        +----------------+ +-----------+ +----------------+
        | PigLatinLexer  | | YLangLexer| | ZetarianoLexer |
        | PigLatinParser | | YLangParser| | ZetarianoParser|
        +----------------+ +-----------+ +----------------+
                   |             |             |
                   v             v             v
      +------------------+ +---------------+ +------------------+
      | PigLatinASTBuilder    | YLangASTBuilder  | ZetarianoASTBuilder |
      +------------------+ +---------------+ +------------------+
                   |             |             |
                   +------+------+             |
                          |                    |
                          +------------+-------+
                                       v
                       +-------------------------------+
                       |   ast.* (AST UNICO)          |
                       |   program/ expr/ stmt/ decl/ |
                       +-------------------------------+
                                       v
                       +-------------------------------+
                       | SemanticAnalyzer (compartido) |
                       |   Pase A: firmas/declaraciones|
                       |   Pase B: verificación        |
                       |   sobre .pig + imports .y/.z  |
                       +-------------------------------+
                                       v
                       +--------------------+
                       | Cuartetas (IR)     |
                       +--------------------+
                                       v
                       +--------------------+
                       | C3D                |
                       +--------------------+
                                       v
                       +--------------------+
                       | Código C           |
                       +--------------------+
```

### Los dos pases (reemplazan la antigua etapa "Merge")

- **Pase A — Declaración (superficial, sin revisar cuerpos).** Recorre los `ProgramNode`
  de **todos** los archivos (.pig + imports) y registra en la `SymbolTable` global SOLO las
  firmas:
  - `StructDeclNode` → símbolo `ESTRUCTURA` (+ un ámbito nuevo con sus campos `isField`).
  - `FunctionDeclNode` → símbolo `FUNCION` (tipo de retorno, nº de parámetros).
  - `ClassDeclNode` → símbolo `CLASE` (+ un ámbito nuevo con atributos `isField`, métodos
    `METODO` y constructores `CONSTRUCTOR`).
- **Pase B — Verificación.** Llama `programa.analizar(contexto)` sobre cada programa en
  cualquier orden. Como Pase A ya registró las firmas de todo lo importado, una llamada del
  `.pig` a `calcularPoder(fuerza)` resuelve aunque el archivo `.y` se procese después.

La resolución de imports la orquesta la UI: el `.pig` se parsea primero; por cada
`ImportNode.rutaCompleta` (p.ej. `alienes.persona`) se busca `<carpetaProyecto>/alienes/persona.y`
o `...persona.z`, se construye su AST con el builder correspondiente y se agrega a la lista.

- [ ] IMPORTANTE (pendiente): el alcance acordado de `Correccion...md` menciona aplicar Pase A
      recursivo a imports-de-imports; el spec actual no lo exige, documentarlo.

---

## 3. Especificaciones de los lenguajes

Las especificaciones completas están en `C:\Users\Anthony\Downloads\`:

- `PigLatin_Especificacion.md` — spec de `.pig`.
- `YLang_Especificacion.md` — spec de `.y`.
- `Zetariano.g4` — la gramática real de `.z` hace de spec (no hay `Zetariano_Especificacion.md`).

Resumen operativo (lo que la gramática ANTLR del proyecto implementa):

### 3.1 Pig Latin (`.pig`)

- Estructura: `[imports]` → `[VARIABILES> ...]` → `MAIOR> ... FINIS ;` (fin de programa =
  `FINIS` mayúscula, token distinto de `finis` minúscula que cierra bloques internos).
- Importaciones: `import ruta.con.puntos` (archivo `.y` o `.z`, el `;` final es opcional).
- Declaraciones globales (solo en `VARIABILES>`):
  - `esto x : numerus 20 ;`
  - `esto x : falsus ;` / `esto x : verum ;`
  - `esto x : novus Clase(args) ;`
  - `esto x : Clase {v1, v2, ...} ;` (literal de estructura posicional)
  - `esto x : Clase ;`
  - `series x[3] : numerus {1,2,3} ;` (arreglo)
- Tipos: `numerus`, `decimalis`, `textum`, `littera`, `bool` + cualquier identificador
  (estructura/clase importada).
- E/S: `>> "texto" >> expr ;` (imprimir, encadenable), `x << ;` (leer) o `<< ;`.
- Control: `si (cond) { ... } aliter (cond) { ... } aliter { ... } finis ;`;
  `dum (cond) { ... } finis ;`; `facere { ... } dum (cond) ;`;
  `per (esto i : numerus 0; cond; i++) { ... } finis ;` (el `finis;` final es opcional en `per`);
  `perge ;` (continue), `interrumpe ;` (break).
- Expresiones: `||`, `&&`, `==`, `!=`, `<`, `>`, `<=`, `>=`, `+`, `-`, `*`, `/`,
  unario `non` (negación) y `-`. Accesos encadenados: `a.b`, `a[i]`, `f(x)`, combinables.
- Literales: enteros, decimales, `"cadena"`, `'c'`, `verum`, `falsus`, `novus Clase(...)`,
  literal de estructura/anidado `{...}`, `(expr)`.
- Comentarios: `//` y `## ... ##` (canal HIDDEN). BLOCK:
- NO hay declaraciones locales dentro de `MAIOR>` (solo globales en `VARIABILES>`); no hay
  funciones/estructuras en `.pig`.

### 3.2 Y? (`.y`)

- Estructura: `[%estructuras ...]` → `%funciones ...` (obligatoria). SIN llaves ni `;`;
  bloques por **indentación** con tokens sintéticos `INDENT`/`DEDENT`.
- Estructuras: `estructura Nombre:` + campos indentados `tipo nombre` (o `tipo nombre[N]`
  arreglo de tamaño literal; 1+ dimensiones).
- Funciones: `definir nombre(params) :` con cuerpo indentado; tipo de retorno opcional
  `-> tipo :`; parámetros con prefijo `[]` (arreglo por referencia) o `{}` (estructura por
  referencia) o directos (primitivo por valor).
- Tipos: `entero`, `flotante`, `cadena`, `caracter`, `bool` + nombre de estructura.
- Sentencias: declaración local `tipo nombre (= expr)` / `tipo nombre[N]`, asignación,
  llamada `identificador(args)` (incluye built-ins `imprimir(...)` y `leer()`, que NO son
  palabras reservadas), `si (c) entonces ... sino (c) entonces ... contrario ...`,
  `elegir (e): caso N: ... siempre: ...` (con `romper`), `para (...):`,
  `mientras (c) hacer ...`, `hacer: ... mientras (c)`, `retornar expr`, `romper`, `continuar`.
- Operadores: `||`, `&&`, `==`, `!=`, `<`, `>` (NO existen `<=` ni `>=`), `+` `-` `*` `/`,
  unario `!` y `-`.
- Literales: `{...}` es literal de arreglo o de estructura (se resuelve por el tipo
  declarado en semántica). Comentarios `//` y `/* ... */`.
- Convención de paso: primitivos por valor; `[]`/`{}` siempre por referencia.

### 3.3 Zetariano (`.z`)

- Estructura: `public class Nombre { miembros }` (una sola clase; validación
  archivo==nombre de clase se difiere a semántica/carga). Sin encapsulamiento ni herencia.
- Miembros: campo `tipo nombre = expr? ;`, constructor `public Nombre(params) { ... }`,
  método `public (void | tipo) nombre(params) { ... }`. Arreglos estilo Java pegados al
  tipo: `int[] numeros`.
- Tipos base: `int`, `double`, `char`, `boolean`, `String` + cualquier identificador
  (otra clase).
- Sentencias: declaración local, asignación, asignación compuesta `+=` `-=` `*=`,
  `++`/`--`, llamada como sentencia, `if`/`else if`/`else`, `switch`/`case`/`default`
  (fallthrough real), `for` clásico, `while`, `do/while`, `return exprl?`, `break`,
  `continue`, bloque anidado.
- **Los cuerpos de `if`/`else`/`for`/`while`/`do` pueden ser un bloque `{ }` O una sola
  sentencia sin llaves** (`cuerpoOSentencia`).
- `for`: `for (tipo x = expr; cond; paso)`, `for (x = expr; cond; paso)` o
  `for (; cond; )` (inits/updates opcionales; `forUpdate` puede ser `++`/`--` o asignación).
- Expresiones: ternario `a ? b : c` en la cima, `||`, `&&`, `==`/`!=`, `<`/`>`/`<=`/`>=`,
  `+`/`-`, `*`/`/`/`%`, unario `!`/`-`, accesos encadenados.
- Factores: `NUMERO`, `DECIMAL`, `CADENA`, `CARACTER`, `true`, `false`, `null`,
  `new Clase(args)`, `new int[5]`, `new int[3][3]`, `new Clase[...]`,
  `{1,2,3}` (literal de arreglo), `(expr)`.

---

## 4. Stack tecnológico y build

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 (`maven.compiler.release=21`) | Lenguaje de implementación |
| ANTLR | 4.13.1 | Lexers, parsers y visitantes (`visitor=true`, `listener=true`) |
| RSyntaxTextArea | 3.3.4 | Editor con resaltado |

`pom.xml` (raíz del módulo `Alien_code`):
- `groupId=cunoc.compi2`, `artifactId=Alien_code`, `packaging=jar`.
- `exec.mainClass=cunoc.compi2.alien_code.Alien_code`.
- `antlr4-maven-plugin`: `sourceDirectory=${basedir}/src/main/antlr4`,
  `outputDirectory=${project.build.directory}/generated-sources/antlr4`.
- `build-helper-maven-plugin`: agrega `generated-sources/antlr4` al sourcepath.
- `maven-compiler-plugin` 3.12.1 con `<release>21</release>`.

Comandos:

```
mvn -B clean compile        # regenera ANTLR + compila
mvn exec:java               # arranca la GUI (aquí se prueba todo)
```

---

## 5. Estructura del repositorio (100% real)

```
3DAlien/
├── .gitignore
├── MANUAL_TECNICO.md                  # este documento (raíz del repo)
├── Alien_code/
│   ├── pom.xml
│   └── src/
│       ├── main/antlr4/cunoc/compi2/alien_code/
│       │   ├── pigLatin/grammar/PigLatin.g4          # terminada
│       │   ├── ylang/grammar/YLangLexer.g4           # lexer con INDENT/DEDENT
│       │   ├── ylang/grammar/YLangParser.g4          # tokenVocab=YLangLexer
│       │   └── zetariano/grammar/Zetariano.g4        # gramática real de .z
│       └── main/java/cunoc/compi2/alien_code/
│           ├── Alien_code.java                       # main → VentanaPrincipal
│           ├── ast/                                  # AST UNICO
│           │   ├── Node.java
│           │   ├── ASTVisitor.java
│           │   ├── Type.java
│           │   ├── program/  ProgramNode, ImportNode
│           │   ├── decl/     StructDeclNode, FunctionDeclNode, ClassDeclNode,
│           │   │             ConstructorDeclNode, MethodDeclNode, ParameterNode
│           │   ├── expr/     AccessNode, BinaryOpNode, UnaryOpNode, LiteralNode,
│           │   │             NewObjectNode, StructLiteralNode
│           │   └── stmt/     VariableDeclNode, ArrayDeclNode, AssignmentNode,
│           │                 IncrementNode, DecrementNode, BlockNode, IfNode,
│           │                 ElseIfNode, WhileNode, DoWhileNode, ForNode,
│           │                 BreakNode, ContinueNode, PrintNode, ReadNode
│           ├── semantic/                             # SEMANTICA UNICA
│           │   ├── ContextoSemantico.java            # interfaz
│           │   ├── ContextoSemanticoImpl.java
│           │   ├── SemanticAnalyzer.java             # Pase A + Pase B
│           │   ├── Symbol.java                       # + Kind, tipoNombre, miembros
│           │   ├── Scope.java                        # LinkedHashMap + orden
│           │   ├── SymbolTable.java
│           │   └── TypeCompat.java                   # reglas de compatibilidad de tipos
│           ├── pigLatin/astbuilder/PigLatinASTBuilder.java
│           ├── pigLatin/semantic/PigLatinVocabulary.java
│           ├── ylang/astbuilder/YLangASTBuilder.java           # esqueleto
│           ├── ylang/semantic/YLangVocabulary.java
│           ├── zetariano/astbuilder/ZetarianoASTBuilder.java   # esqueleto
│           ├── zetariano/semantic/ZetarianoVocabulary.java
│           ├── ir/        CodigoContexto, Cuarteta, IntermediateCodeGenerator
│           ├── c3d/       C3DInstruction, C3DGenerator
│           ├── codegen/   CCodeGenerator
│           ├── errors/    ErrorType, CompilerError, ErrorListener
│           └── ui/        VentanaPrincipal, VentanaMenuBar, VentanaReporte,
│                          MainPanel, PanelArbolProyecto, PanelEditorTabs,
│                          EditorPanel, PanelLog, VerificadorSintactico,
│                          ProyectoTokenMakerFactory, PigLatinTokenMaker,
│                          YLangTokenMaker, ZetarianoTokenMaker
└── (docs del curso fuera del repo: Downloads\*_Especificacion.md, Zetariano.g4,
    Correccion_AST_Unificado_y_Merge.md)
```

No existe `src/test`: las pruebas se hacen **manualmente desde la GUI**. No se agregan
tests automatizados.

NOTA: NO existen paquetes `pigLatin.ast`, `ylang.ast` ni `zetariano.ast`: fueron eliminados
en la corrección. Todo import de nodos es `cunoc.compi2.alien_code.ast.*`.

---

## 6. Referencia del contrato raíz del AST (`ast`)

### 6.1 `Node` (interfaz raíz de todo nodo)

```java
package cunoc.compi2.alien_code.ast;

public interface Node {
    <T> T accept(ASTVisitor<T> visitor);   // patrón visitante
    String traducir(CodigoContexto ctx);   // generación de cuartetas (HOY return null)
    int getLine();                          // línea base 1
    int getColumn();                        // columna base 1
    Type analizar(ContextoSemantico ctx);   // semántica (23 nodos hechos; `decl` stub)
}
```

### 6.2 `Type` (enum compartido)

Valores: `INT, FLOAT, STRING, BOOL, CHAR, VOID, STRUCT, CLASS, ARRAY`.

Mapeos estáticos (con `name.toLowerCase()`):
- `fromPigLatin`: `numerus`→INT, `decimalis`→FLOAT, `textum`→STRING, `littera`→CHAR,
  `bool`→BOOL.
- `fromZetariano`: `int`→INT, `double`→FLOAT, `string`→STRING, `boolean`→BOOL,
  `char`→CHAR, `void`→VOID.
- `fromYLang`: `entero`→INT, `decimal`→FLOAT, `texto`→STRING, `booleano`→BOOL,
  `caracter`→CHAR, `vacio`→VOID.

- [ ] **PENDIENTE**: `fromYLang` usa nombres del borrador viejo. El spec usa
      `flotante`→FLOAT, `cadena`→STRING, `bool`→BOOL. Hay que alinear (mantener o no los
      antiguos es decisión del plan; el spec manda).
- [ ] **PENDIENTE**: no existe mapeo para `ARRAY` ni para buscar tipos con nombre
      definido en otro archivo (STRUCT/CLASS) — los builders resuelven eso con
      `Type.STRUCT`/`Type.CLASS` + `tipoNombre`/`nombreClase`.

### 6.3 `ASTVisitor<T>`

Interfaz con un método por tipo de nodo. Todos son `default` y devuelven `null` salvo
`visitProgram` (abstracto). Métodos:

`visitProgram(ProgramNode)`, `visitImport(ImportNode)`,
`visitVariableDecl`, `visitArrayDecl`, `visitStructLiteral`, `visitBlock`, `visitAccess`,
`visitNewObject`, `visitAssignment`, `visitIncrement`, `visitDecrement`, `visitRead`,
`visitPrint`, `visitWhile`, `visitDoWhile`, `visitFor`, `visitContinue`, `visitBreak`,
`visitIf`, `visitElseIf`, `visitLiteral`, `visitBinaryOp`, `visitUnaryOp`,
`visitStructDecl(StructDeclNode)`, `visitFunctionDecl(FunctionDeclNode)`,
`visitClassDecl(ClassDeclNode)`, `visitConstructorDecl(ConstructorDeclNode)`,
`visitMethodDecl(MethodDeclNode)`, `visitParameter(ParameterNode)`.

---

## 7. Nodos del AST — referencia de campos y constructores

Convención: campos públicos (POJO), línea/columna `final private` con getters. Cada nodo
implementa `accept` (con su `visit...`), `getLine` y `getColumn`. `traducir` **sigue como
stub** (`return null`) en todos. `analizar` está implementado en los nodos de la fase
semántica de Pig Latin (ver 8.7–8.8); en `decl` sigue stub.

### 7.1 `program`

| Clase | Campos públicos | Constructor |
|---|---|---|
| `ProgramNode` | `List<Node> declarations`, `String sourceLanguage` | `(int line, int col)` |
| `ImportNode` | `String rutaCompleta` | `(String rutaCompleta, int line, int col)` |

`ProgramNode.sourceLanguage` se fija en los builders: `"Pig Latin"` (y, a futuro,
`"Y?"` / `"Zetariano"`). `declarations` contiene imports + declaraciones de nivel
superior + (para Pig Latin) las sentencias de `MAIOR>`.

### 7.2 `decl`

| Clase | Campos públicos | Constructor |
|---|---|---|
| `StructDeclNode` | `String nombre`, `List<VariableDeclNode> campos` | `(nombre, campos, line, col)` |
| `FunctionDeclNode` | `String nombre`, `List<ParameterNode> parametros`, `Type tipoRetorno`, `BlockNode cuerpo` | `(nombre, parametros, tipoRetorno, cuerpo, line, col)` |
| `ClassDeclNode` | `String nombre`, `List<VariableDeclNode> atributos`, `List<ConstructorDeclNode> constructores`, `List<MethodDeclNode> metodos` | `(nombre, atributos, constructores, metodos, line, col)` |
| `ConstructorDeclNode` | `List<ParameterNode> parametros`, `BlockNode cuerpo` | `(parametros, cuerpo, line, col)` |
| `MethodDeclNode` | `String nombre`, `List<ParameterNode> parametros`, `Type tipoRetorno`, `BlockNode cuerpo` | `(nombre, parametros, tipoRetorno, cuerpo, line, col)` |
| `ParameterNode` | `Type tipo`, `String nombre`, `boolean porReferencia` | `(tipo, nombre, porReferencia, line, col)` |

`tipoRetorno == null` significa "sin retorno" (Y? sin `->`, método `void`) → se trata como
`VOID` (ver `SemanticAnalyzer.registrarFirma`).

### 7.3 `expr`

| Clase | Campos públicos | Constructor / notas |
|---|---|---|
| `AccessNode` | `String nombre`, `List<Sufijo> sufijos` | `(nombre, line, col)`; método `conSufijos()`. `Sufijo` interno: `enum Tipo {CAMPO, INDICE, LLAMADA}`, campos `tipo`, `nombreCampo`, `indice` (Node), `argumentos` (List<Node>); factories estáticas `Sufijo.campo(name)`, `Sufijo.indice(node)`, `Sufijo.llamada(list)` |
| `BinaryOpNode` | `String operador`, `Node izquierda`, `Node derecha` | `(operador, izquierda, derecha, line, col)`. `operador` guarda el texto crudo: `||`, `&&`, `==`, `!=`, `<`, `>`, `<=`, `>=`, `+`, `-`, `*`, `/`, (y a futuro `%`) |
| `UnaryOpNode` | `String operador`, `Node operando` | `(operador, operando, line, col)`. `operador`: `non`, `!` o `-` |
| `LiteralNode` | `enum Clase {ENTERO, DECIMAL, CADENA, CARACTER, BOOLEANO}`, `Clase clase`, `String valor` | `(clase, valor, line, col)`. `valor` es el texto crudo (incluye comillas para CADENA/CARACTER si así lo emite el builder) |
| `NewObjectNode` | `String nombreClase`, `List<Node> argumentos` | `(nombreClase, argumentos, line, col)`. Instancia de objeto (`new Clase(...)` / `novus Clase(...)`) |
| `StructLiteralNode` | `List<Node> valores` | `(valores, line, col)`. Literal `{...}` posicional (estructura o arreglo según contexto) |

### 7.4 `stmt`

| Clase | Campos públicos | Constructor |
|---|---|---|
| `VariableDeclNode` | `String nombre`, `Type tipo`, `String tipoNombre`, `Node inicial` | `(nombre, tipo, tipoNombre, inicial, line, col)` |
| `ArrayDeclNode` | `String nombre`, `int tamano`, `Type tipo`, `String tipoNombre`, `List<Node> iniciales` | `(nombre, tamano, tipo, tipoNombre, iniciales, line, col)` |
| `AssignmentNode` | `AccessNode destino`, `Node valor` | `(destino, valor, line, col)` |
| `IncrementNode` | `AccessNode objetivo` | `(objetivo, line, col)` |
| `DecrementNode` | `AccessNode objetivo` | `(objetivo, line, col)` |
| `BlockNode` | `List<Node> sentencias` | `(line, col)` (inicializa la lista) |
| `IfNode` | `Node condicion`, `BlockNode cuerpo`, `List<ElseIfNode> ramas` | `(condicion, cuerpo, ramas, line, col)` |
| `ElseIfNode` | `Node condicion`, `BlockNode cuerpo` | `(condicion, cuerpo, line, col)`; `esElse() = condicion==null` |
| `WhileNode` | `Node condicion`, `BlockNode cuerpo` | `(condicion, cuerpo, line, col)` |
| `DoWhileNode` | `BlockNode cuerpo`, `Node condicion` | `(cuerpo, condicion, line, col)` |
| `ForNode` | `Node inicial`, `Node condicion`, `Node paso`, `BlockNode cuerpo` | `(inicial, condicion, paso, cuerpo, line, col)` |
| `BreakNode` | — | `(line, col)` |
| `ContinueNode` | — | `(line, col)` |
| `PrintNode` | `List<Node> expresiones` | `(expresiones, line, col)` — imprime varias expresiones encadenadas (`>> a >> b`) |
| `ReadNode` | `AccessNode destino` (puede ser `null`: `<< ;` sin variable) | `(destino, line, col)` |

NOTA `tipoNombre`: en `VariableDeclNode`/`ArrayDeclNode`/`ParameterNode` y builders de Pig
Latin se usa cuando el tipo es un identificador (estructura/clase): `tipo=Type.STRUCT` y
`tipoNombre="Persona"`. Así se conserva el nombre original para resolver por nombre en
semántica (la tabla se indexa por nombre).

### 7.5 GAPS del AST frente a las gramáticas (CRITICO para planear fases)

La gramática de Zetariano/Y? acepta construcciones que **el AST aún no puede representar**.
Cada gap requiere decidir un nodo nuevo o un desglose ("desugar") dentro del builder. Lista:

| # | Feature (de qué gramática) | Gap | Opciones a decidir |
|---|---|---|---|
| 1 | `retornar expr` (Y?), `return expr?` (Z) | No existe `ReturnNode` | Nodo nuevo `ReturnNode(Node expr)` |
| 2 | `elegir`/`caso`/`siempre` (Y?), `switch`/`case`/`default` (Z) | No existe nodo switch | Nodo nuevo `SwitchNode` (o reutilizar `IfNode` encadenado en el builder, aunque el spec exige fallthrough) |
| 3 | Ternario `a ? b : c` (Z) | No existe expresión condicional | Nodo nuevo `ConditionalNode` |
| 4 | `null` (Z) | `LiteralNode.Clase` no tiene NULO | Agregar `Clase.NULO` (o nodo `NullNode`) |
| 5 | `new int[5]`, `new int[3][3]`, `new Clase[]` (Z) | `NewObjectNode` solo instancia objetos con `(...)` | Nodo nuevo para creación de arreglo |
| 6 | Asignación compuesta `+=` `-=` `*=` (Z) | `AssignmentNode` solo `=` | Desugar a `destino = destino op valor` o nodo nuevo |
| 7 | Arreglos multidimensionales `entero m[3][3]` (Y), `int[][]` (Z) | `ArrayDeclNode.tamano` es un solo `int`; `VariableDeclNode` no guarda dims | Agregar `List<Integer> dimensiones` (o nodo/tipo `ARRAY` anidado) |
| 8 | Caracteres/arreglos en **campos de estructura y parámetros** (Y: `tipoCampo IDENTIFICADOR ([N])*`; Z: `int[] arr`) | Campos y parámetros usan `VariableDeclNode`/`ParameterNode` sin dims | Soporte de dimensiones en campos/parámetros |
| 9 | Cuerpo sin llaves de `if`/`for`/`while`/`do` (Z) | Los cuerpos son `BlockNode` | Envolver la sentencia única en `BlockNode` de 1 elemento (decisión del builder) |
| 10 | `imprimir(...)`/`leer()` (Y) | Están cubiertos por `AccessNode` con sufijo LLAMADA, pero requieren resolución como built-in en semántica | Marcar en semántica, no en AST |
| 11 | `for` con `forInit` opcional o como asignación (Z) | `ForNode.inicial` es `Node`, cubre ambos | Sin gap estructural (decisiones del builder) |

Este manual NO decide los gaps: el planificador elige el diseño más simple y lo justifica
en el plan (de preferencia, agregar el nodo al paquete `ast` una sola vez y que los 3
builders lo reutilicen).

---

## 8. Referencia de la semántica (`semantic`)

### 8.1 `Symbol`

```java
public class Symbol {
    public enum Kind { VARIABLE, FUNCION, ESTRUCTURA, CLASE, METODO, CONSTRUCTOR }
    // constructor: (String name, Type type, Kind kind,
    //               boolean isArray, boolean isParameter, boolean isField, int size)
    public String getName();  public Type getType();  public Kind getKind();
    public boolean isArray(); public boolean isParameter(); public boolean isField();
    public int getSize();  // arreglo → tamaño; struct/función/clase → nº de campos/params/atributos

    // Adiciones de la fase semántica (no rompen el constructor ni las llamadas existentes):
    public String getTipoNombre(); public void setTipoNombre(String tipoNombre);
    public Scope getMiembros();   public void setMiembros(Scope miembros);
}
```

`tipoNombre` guarda el nombre real cuando `type` es `STRUCT`/`CLASS` (si no, `null`).
`miembros` es el `Scope` con los campos/métodos/constructores, adjuntado por
`SemanticAnalyzer.paseA` solo a los símbolos `ESTRUCTURA`/`CLASE`.

### 8.2 `Scope`

Ámbito con padre y `Map<String,Symbol>` (`LinkedHashMap`). `define` inserta en el mapa y
además agrega a una lista `orden`, de modo que `getTodos()` devuelve los símbolos **en
orden de declaración** y **conserva los duplicados** por nombre (necesario para los
constructores, que comparten el nombre de la clase). `getTodos()` es la base de
`StructLiteralNode.validarContra` y del filtro de constructores en `NewObjectNode`.

### 8.3 `SymbolTable`

Pila de `Scope` (en el constructor ya entra el ámbito global). API:

- `enterScope()`, `exitScope()` (no desapila el último: el global nunca se cierra).
- `isDefinedInCurrentScope(name)`, `define(symbol)`, `resolve(name)`
  (del ámbito actual hacia afuera).
- `listarSimbolos()` → `List<Symbol>` deduplicada por nombre (recorre ámbitos
  abajo→arriba), usada por el reporte de la tabla de símbolos de la UI.

### 8.4 `ContextoSemantico` (interfaz) y `ContextoSemanticoImpl`

```java
public interface ContextoSemantico {
    Type evaluar(Node nodo);            // nodo.analizar(this)
    void entrarAmbito();  void salirAmbito();
    boolean definir(Symbol simbolo);    // false si ya existe en el ámbito actual
    Symbol resolver(String nombre);
    void registrarError(int linea, int columna, String mensaje);   // ErrorType.SEMANTICO
    void entrarCiclo();  void salirCiclo();  boolean enCiclo();     // para romper/continuar
}
```

`ContextoSemanticoImpl` implementa todo sobre `SymbolTable` + `ErrorListener`, con un
contador `loopDepth`. También expone `getSymbolTable()`.

### 8.5 `SemanticAnalyzer` (fachada del pipeline)

```java
public class SemanticAnalyzer {
    public SemanticAnalyzer(ErrorListener errorListener);
    public void analizar(List<ProgramNode> programas);     // paseA + paseB
    public void paseA(List<ProgramNode> programas);
    public void paseB(List<ProgramNode> programas);
    public SymbolTable getSymbolTable();
}
```

`paseA` → para cada `programa.declarations`, `registrarFirma(nodo)`. **Los miembros ya no se
descartan:** se construye un `Scope` propio (padre `null`), se define ahí cada miembro y el
`Scope` queda adjunto al símbolo con `setMiembros(...)`:
- `StructDeclNode`: define `(nombre, STRUCT, ESTRUCTURA, size=campos.size)`; crea el `Scope`
  de miembros; define cada campo `(campo.nombre, campo.tipo, VARIABLE, isField=true)` con
  `setTipoNombre` si el campo es `STRUCT`/`CLASS`; `simbolo.setMiembros(scope)`.
- `FunctionDeclNode`: define `(nombre, retorno ?? VOID, FUNCION, size=parametros.size)`.
- `ClassDeclNode`: define `(nombre, CLASS, CLASE, size=atributos.size)`; crea el `Scope` de
  miembros; define atributos (VARIABLE, isField), métodos `(nombre, retorno, METODO)` y
  constructores `(clase.nombre, VOID, CONSTRUCTOR, size=parametros.size)`;
  `simbolo.setMiembros(scope)`.

`paseB` → `programa.analizar(contexto)` para cada programa (los bodies ya pueden resolver).

### 8.6 Vocabularies (una por idioma, en `<lenguaje>/semantic`)

Cada una es una clase `final` con constructor privado y `static Type tipoDe(String nombre)`
que delega en `Type.from<Idioma>`. Son la única duplicación legítima.

### 8.7 Estado semántico

- **Hecho (fase semántica Pig Latin, spec `Semantico_PigLatin_v2.md`):**
  - `TypeCompat` creado en `semantic/` (compatibilidad/asignabilidad, aritmética,
    comparación y orden).
  - `Symbol` extendido con `tipoNombre`/`miembros`; `Scope` con orden de declaración.
  - `SemanticAnalyzer.paseA` adjunta el `Scope` de miembros a cada `ESTRUCTURA`/`CLASE`.
  - `analizar(ContextoSemantico)` implementado en **23 nodos** (raíz, `stmt` y `expr`;
    ver 8.8). Los campos/`traducir` siguen igual.
  - Regla transversal: `null` = "tipo desconocido, error ya reportado" → se propaga sin
    generar un segundo error.
- **Pendiente:**
  - `analizar()` en los nodos `decl` (`StructDeclNode`, `FunctionDeclNode`,
    `ClassDeclNode`, `MethodDeclNode`, `ConstructorDeclNode`, `ParameterNode`): sin esto
    los **cuerpos de funciones/métodos no se analizan**.
  - Validación de firmas por **tipos** (no solo cantidad): requiere `List<Type>
    tiposParametros` en `Symbol` para `FUNCION`/`METODO`.
  - Built-ins `imprimir`/`leer` en Y? (para Pig Latin van por `PrintNode`/`ReadNode`).
  - Alcance semántico para árboles de Y? y Zetariano (sus builders están esqueleto).
- La tabla de compatibilidad de tipos (antes "pendiente") ahora vive en
  `semantic/TypeCompat`: único ensanchamiento `INT`→`FLOAT`; `+` sobre `STRING` concatena;
  comparación `==`/`!=` admite iguales o numéricos; `<`/`>` solo numéricos.

### 8.8 Nodos con `analizar()` implementado (fase Pig Latin)

| Grupo | Nodos |
|---|---|
| Raíz | `ProgramNode`, `ImportNode`, `BlockNode` |
| Declaraciones | `VariableDeclNode`, `ArrayDeclNode`, `StructLiteralNode` (+ `validarContra(Symbol,ContextoSemantico)`) |
| Expresiones | `LiteralNode`, `BinaryOpNode`, `UnaryOpNode`, `AccessNode`, `NewObjectNode` |
| Sentencias | `AssignmentNode`, `IncrementNode`, `DecrementNode`, `IfNode`, `ElseIfNode`, `WhileNode`, `DoWhileNode`, `ForNode`, `BreakNode`, `ContinueNode`, `PrintNode`, `ReadNode` |

Patrón general por nodo: se documenta el tipo declarado del destino/símbolo, se
resuelven/reportan errores y se registra el símbolo con `ctx.definir`. Para conocer el tipo
de un hijo se usa `ctx.evaluar(hijo)` (equivale a `hijo.analizar(ctx)`).

**Limitaciones conocidas (seguir en fases posteriores):** los cuerpos de
funciones/métodos no se analizan (nodos `decl` stub); las llamadas a función/método solo
verifican que existan, no la firma completa; la compatibilidad de `STRUCT`/`CLASS` que
llega como resultado de un `AccessNode` compara solo el `Type` (no el nombre), porque
`Type` es un enum plano.

---

## 9. Gramáticas ANTLR — notas de diseño relevantes para planes

### 9.1 PigLatin.g4 (terminada, probada indirectamente)

- Marcadores `VARIABILES>`/`MAIOR>` son tokens propios (`VARIABILES_MARKER`,
  `MAIOR_MARKER`). `FINIS` (mayúsc.) y `finis` son tokens distintos.
- Todo el caso de bloques se maneja con `cuerpoBloque = { ... }`. `per` deja el
  `finis;` final opcional.
- `imprimir : ESCRIBIR (ESCRIBIR? expresion)+ PUNTO_COMA ;` (`>> "a" >> x ;`).
- Comentarios `//` y `##...##` en canal `HIDDEN`.

### 9.2 YLangLexer.g4 + YLangParser.g4 (terminada y probada)

- **Lexer de solo lexer** (`lexer grammar`) que declara `tokens { INDENT, DEDENT }` y
  sobrescribe `nextToken()` con: pila de indentación (`indentStack`), `opened` (no genera
  INDENT/DEDENT dentro de `(`/`[`), cola de tokens (`tokenQueue`), y cierre de niveles en
  `EOF` (`eofHandled`). La separación lexer/parser es obligatoria: en una gramática
  combinada los tokens sintéticos no existen en el código Java del lexer. Los bloques son
  `NEWLINE INDENT sentencia+ DEDENT`.
- `NEWLINE` salta líneas en blanco/comentarios y captura la indentación final de la línea.
- Operadores relacionales de Y? son SOLO `<` `>` (no hay `MENOR_IGUAL`/`MAYOR_IGUAL`).
- `imprimir`/`leer` NO son tokens: se parsean como `accesoVariable` con sufijo de llamada.

### 9.3 Zetariano.g4 (terminada y probada, es la gramática real de los alumnos)

- `programa : PUBLIC CLASS IDENTIFICADOR '{' miembroClase* '}' EOF` — validar que el
  archivo se llama igual que la clase es responsabilidad de la fase de carga/semántica.
- `metodo : PUBLIC (VOID | tipo arrayDims?) IDENTIFICADOR '(' parametros? ')' bloque`.
- `cuerpoOSentencia : bloque | sentencia` (cuerpos sin llaves).
- `%` = `MODULO`, asignaciones compuestas `MAS_ASIGNA`/`MENOS_ASIGNA`/`POR_ASIGNA`,
  ternario en `expresion`.
- `string` es el token `STRING` (`'String'`), que es **mayúscula**: por eso
  `Type.fromZetariano` usa `toLowerCase()`.
- `new <tipoBase>[...]` y `new Clase[...]` usan el mismo token `NEW`.

---

## 10. Builders (`<lenguaje>/astbuilder`) — estado y requisitos

### 10.1 `PigLatinASTBuilder` (TERMINADO — emite nodos `ast.*`)

Extiende `PigLatinBaseVisitor<Node>`. Método público `ProgramNode construir(String codigo)`:
crea `Lexer`+`CommonTokenStream (DEFAULT_CHANNEL)`+`Parser`, invoca
`visitPrograma(parser.programa())`. Mapeo realizado (referencia para los otros builders):

| ParseTree Pig Latin | Nodo AST |
|---|---|
| `importacion` → `rutaArchivo` (identificadores unidos con `.`) | `ImportNode` |
| `declaracionVariable` (verdadero/falso, tipo primitivo, `novus`, literal struct, tipo identificador) | `VariableDeclNode` (+ `NewObjectNode`/`StructLiteralNode` como `inicial`) |
| `declaracionArreglo` | `ArrayDeclNode` (tipo primitivo o STRUCT+tipoNombre) |
| `leer` (con o sin variable) | `ReadNode` (destino puede ser null) |
| `imprimir` | `PrintNode` |
| `dum`/`facere`/`per` | `WhileNode`/`DoWhileNode`/`ForNode` (p.ej. `inicial` de `per` = `VariableDeclNode`, `paso` = `IncrementNode`/`DecrementNode`) |
| `si`/`aliter` | `IfNode` + `List<ElseIfNode>` (`aliter` sin condición → `ElseIfNode(null, cuerpo)`) |
| `asignacion`/`incremento`/`decremento` | `AssignmentNode`/`IncrementNode`/`DecrementNode` |
| Todos los niveles de `expresion*` | `BinaryOpNode` encadenados con el texto del operador; `non`→`UnaryOpNode("non")`, `-`→`UnaryOpNode("-")` |
| `accesoVariable` + `sufijoAcceso` | `AccessNode` + `Sufijo.campo/indice/llamada` |
| Literales/factores | `LiteralNode` (Clase según token), `NewObjectNode`, `StructLiteralNode` compuesto |

Posición: `line = ctx.getStart().getLine()`, `column = getCharPositionInLine() + 1`.

### 10.2 `YLangASTBuilder` (ESQUELETO — solo `public ProgramNode construir(String) { return null; }`)

Debe extender `YLangBaseVisitor<Node>` y traducir `structura` → `StructDeclNode`,
`funcion` → `FunctionDeclNode` (+ `ParameterNode` con `porReferencia = []=|{}=`), cuerpo de
función → `BlockNode`, y las sentencias Y? a nodos `stmt`/`expr`. Decisiones obligatorias
del plan para TODO lo listado en la sección 7.5 aplicable a Y? (return, switch, dims en
campos/arreglos, etc.). `sourceLanguage = "Y?"`.

### 10.3 `ZetarianoASTBuilder` (ESQUELETO)

Debe traducir `programa` (una clase) → `ClassDeclNode` (atributos, constructores, métodos);
campos → `VariableDeclNode`; métodos/constructores → `MethodDeclNode`/`ConstructorDeclNode`
con `BlockNode`; sentencias/expresiones → `stmt`/`expr` (incl. decisiones de 7.5 para
ternario, `null`, `new int[]`, `+=`, `return`, `switch`, dims, cuerpos sin llaves, `%`).
`sourceLanguage = "Zetariano"`. Para `if` anidado con `else if`: `ramaElse→condicional` se
colapsa a `ElseIfNode` o se anida un `IfNode` dentro de `ramas` (decisión del plan; la
estructura `IfNode+List<ElseIfNode>` es la natural).

---

## 11. Referencia del backend (`ir`, `c3d`, `codegen`)

Todo el backend está **estructurado pero sin lógica completa**.

### 11.1 `ir.CodigoContexto` (contrato de emisión)

```java
public interface CodigoContexto {
    String nuevoTemporal();                       // "t0", "t1", ...
    String nuevaEtiqueta();                       // "L0", "L1", ...
    void emitir(String operador, String operando1, String operando2, String resultado);
    void empujarCiclo(String etiquetaContinuar, String etiquetaSalida);
    void popCiclo();
    String etiquetaContinuarActual();             // null si no hay ciclo
    String etiquetaSalidaActual();
}
```

### 11.2 `ir.IntermediateCodeGenerator` (implementa `CodigoContexto`)

Campos: `List<Cuarteta> cuartetas`, `ErrorListener`, `Deque<String[]> ciclos`,
`tempCounter`, `labelCounter`. API: `getCuartetas()`, `getErrorListener()`. Generación
global NO vinculada a ningún idioma — cada nodo emitirá vía `Node.traducir(ctx)`.
**PENDIENTE**: implementar `traducir` en los 29 nodos. Pauta general para el plan:
recorrer el árbol, `emitir(op, op1, op2, res)`; para el flujo usar `nuevaEtiqueta()` y
`empujarCiclo/etiqueta*Actual` para `romper`/`continuar`.

### 11.3 `ir.Cuarteta`

POJO `(operador, operando1, operando2, resultado)` con `toString()` = `(op, a1, a2, res)`.

### 11.4 `c3d.C3DInstruction` y `c3d.C3DGenerator`

- `C3DInstruction`: `(operador, operando1, operando2, resultado)`; `toString()` =
  `res = op1 operador op2;` o, si `resultado` vacío, `operador op1 op2;`.
- `C3DGenerator`: `getInstructions()`, `newLabel()`, `generate(List<Cuarteta>)` **vacío**
  (PENDIENTE). Es el traductor cuarteta→C3D (p.ej. cuarteta `goto`/`if` → salto C3D).

### 11.5 `codegen.CCodeGenerator`

`generate(List<C3DInstruction>)` hoy solo escribe las cabeceras
`#include <stdio.h>` / `#include <stdlib.h>`. `getCode()`. **PENDIENTE**: emitir código C
a partir de las instrucciones C3D (variables, arreglos, structs/objetos con tipos del
lenguaje destino, funciones/métodos). Decisión del plan: en qué C se bajan structs y
objetos (structs de C; clases → structs + funciones libres o paralelas) y cómo se maneja
la entrada/salida (`printf`/`scanf`, y el "leer" tipado de Pig Latin).

---

## 12. Manejo de errores (`errors`)

- `ErrorType`: `LEXICO`, `SINTACTICO`, `SEMANTICO`.
- `CompilerError`: `(type, message, line, column)`; getters `getType/getMessage/getLine/
  getColumn`; `toString()` = `[TIPO] Línea N, columna M: mensaje`.
- `ErrorListener`: `addError(type, message, line, column)`, `getErrors()`, `hasErrors()`,
  `clear()`, `getFormattedErrors()`.

Usos actuales: `VerificadorSintactico.ErrorSintactico` (clase propia de la UI con campos
`linea/columna/mensaje`, columna ya +1); `ContextoSemanticoImpl` → `ErrorType.SEMANTICO`;
`IntermediateCodeGenerator` recibe un `ErrorListener`.

---

## 13. Interfaz gráfica (`ui`) — lo que el pipeline necesita

### 13.1 `VentanaPrincipal`

Campos de reporte: `ventanaErrores` (`{"Tipo","Descripción","Línea","Columna"}`),
`ventanaSimbolos` (`{"Nombre","Tipo","Clase","Tamaño","Valor","Línea"}`),
`ventanaCuartetas` (`{"#","Operador","Operando1","Operando2","Resultado"}`),
`ventanaC3D` (`{"#","Instrucción"}`). Además `carpetaProyecto` (File, abierta por el árbol).

**Flujo de `compilar(log)` (estado ACTUAL):**

1. `log.limpiar()`, `ventanaErrores.limpiar()`.
2. Editor activo → `VerificadorSintactico.verificar(texto, archivo)`.
3. Si extensión no válida → error y fin.
4. Log tokens + "Análisis sintáctico...". Si hay errores sintácticos → filas en
   `ventanaErrores` y fin.
5. Sin errores sintácticos: si el archivo es `.pig`, `ejecutarPipelineSemantico`:
   - `PigLatinASTBuilder.construir(leer(archivoPig))` (si `null` → error).
   - Por cada `ImportNode` en `programa.declarations`: `resolverImport(rutaCompleta)`
     (`carpetaProyecto/ruta(.y|.z)`; reposa en `carpetaProyecto`, si es `null` falla).
     Si no existe → error en log. Si el builder devuelve `null` → pendiente en log.
   - `SemanticAnalyzer.analizar(programas)`.
   - Errores semánticos → log y (aún NO se llena `ventanaErrores` desde semántica:
     pendiente menor).
   - `mostrarSimbolos(analizador.getSymbolTable())` → `ventanaSimbolos`.
6. Si es `.y`/`.z` → log "pendiente backend".

- [ ] **PENDIENTE (conectar)**: cuartetas a `ventanaCuartetas`, C3D a `ventanaC3D`,
      código C (menú "Ver código C" hoy muestra un placeholder), y reportar errores
      semánticos también en `ventanaErrores`.
- [ ] **PENDIENTE (robustez)**: los builders de los imports se invocan sin verificación
      sintáctica previa. Los planes deben incluir attaching de `ErrorListener` o
      verificación previa para que un `.y`/`.z` mal parseado no rompa el runtime.

### 13.2 `VerificadorSintactico`

`Resultado{extensionValida, lenguaje, cantidadTokens, errores}` con `hayErrores()`.
`verificar(codigo, archivo)` elige por extensión (`verificarY/Z/Pig`); en cada una:
crear lexer+parser, adjuntar `BaseErrorListener` que suma a `errores` (columna+1),
`tokens.fill()`, y si no hay errores sintácticos `parser.reset(); parser.<raiz>();`
`cantidadTokens` = nº tokens − 1 (EOF).

### 13.3 Resto de la UI (resumen)

- `VentanaMenuBar`: menús + botones Compilar/Limpiar; listeners anclados por índice
  (`setOn...`).
- `MainPanel`, `PanelArbolProyecto` (JTree, doble clic abre; menú contextual nuevo/
  renombrar/eliminar; iconos por extensión), `PanelEditorTabs`+`EditorPanel`
  (RSyntaxTextArea, estilo por extensión, barra de línea/columna), `PanelLog`
  (info/éxito/error/pendiente).
- `VentanaReporte`: JDialog con JTable no editable; `setDatos(List<Object[]>)`, `limpiar()`.
- Token makers (`PigLatinTokenMaker`, `YLangTokenMaker`, `ZetarianoTokenMaker`) y
  `ProyectoTokenMakerFactory` (registra `text/ylang`, `text/zetariano`, `text/piglatin`);
  delegan en el lexer ANTLR de cada idioma; `YLangTokenMaker` colorea
  `INDENT/DEDENT/NEWLINE/WS_INTERNO` como espacios.

---

## 14. Verificación (por frontend)

**El proyecto no tiene pruebas automatizadas.** `src/test` fue eliminado y JUnit ya no está
en `pom.xml`. La verificación de cada fase se hace **manualmente desde la GUI**:

1. `mvn -B clean compile` (debe compilar sin errores).
2. `mvn exec:java` → abrir/crear un proyecto, escribir archivos `.pig`/`.y`/`.z` reales.
3. Pulsar **Compilar** y revisar el panel de log, la tabla de símbolos, los errores y
   (cuando existan) los reportes de cuartetas/C3D/código C.

Recomendación para los planes: incluir un **caso de prueba manual** por fase (archivos de
entrada concretos y el resultado esperado en la GUI) en lugar de tests.

Casos manuales que ya deben funcionar (fase semántica Pig Latin): variable no declarada,
asignación de tipo incompatible, condición no booleana, `interrumpe`/`perge` fuera de
ciclo, y literal de estructura con número/tipo de campos incorrectos.

---

## 15. Estado por componente (matriz)

| Componente | Estado |
|---|---|
| Corrección de arquitectura (AST/semántica unificados) | Aplicada y compilando |
| Gramática Pig Latin | Terminada |
| Gramática Y? (INDENT/DEDENT) | Terminada y probada (10) |
| Gramática Zetariano (real) | Terminada y probada (10) |
| AST unificado (29 clases) + `decl/` | Terminado |
| `PigLatinASTBuilder` | Terminado (emite `ast.*`) |
| `YLangASTBuilder` / `ZetarianoASTBuilder` | Esqueletos (`construir` → null) |
| Pase A (firmas + miembros en `Symbol`) + Pase B | Operativos; Pase B analiza los 23 nodos de Pig Latin |
| `Node.analizar` | Implementado en 23 nodos (fase Pig Latin); `decl` sigue stub |
| `TypeCompat` / `Symbol.tipoNombre,miembros` / `Scope` ordenado | Terminados |
| `Node.traducir` (29 nodos) | Stub (`return null`) |
| `C3DGenerator.generate` | Vacío |
| `CCodeGenerator.generate` | Solo cabeceras |
| Resolución de imports + tabla de símbolos en la UI | Operativa (`.pig`) |
| Reportes de cuartetas/C3D/código C en la UI | No conectados |
| Pipeline UI | Parcial: `.pig` → sintaxis + imports + 2 pases + símbolos |

---

## 16. Pendientes priorizados y dependencias (entrada para las fases)

Orden recomendado de fases (cada una debe terminar compilando y verificada desde la GUI):

1. **P1 — Alineamiento de tipos y dims del AST** (sin lógica nueva):
   - Alinear `Type.fromYLang` (`flotante/cadena/bool`).
   - Decidir y agregar soporte de dimensiones de arreglo (gap 7–8) y ajustar
     `ArrayDeclNode`/campos/parámetros. *Depende de nada; desbloquea builders.*
2. **P2 — Nodos faltantes por decisión** (gap 1–6): `ReturnNode`, `SwitchNode` (o desugar),
   `ConditionalNode`, `Clase.NULO`, arr‑new, desugar de `+=`; registrar en `ASTVisitor`.
   *Depende de P1 (dims si switch/arr‑new los necesitan).*
3. **P3 — `YLangASTBuilder`** completo (vitando P1/P2) + caso manual en la GUI.
4. **P4 — `ZetarianoASTBuilder`** completo + caso manual en la GUI.
5. **P5 — Semántica (HECHA en Pig Latin; falta extenderla)**:
   - Hecho (spec `Semantico_PigLatin_v2.md`): `TypeCompat`, `Symbol` (`tipoNombre`/
     `miembros`), `Scope` ordenado, `paseA` con miembros en el `Symbol`, y `analizar()` en
     23 nodos; verificada desde la GUI.
   - Pendiente: `analizar()` en los nodos `decl` (cuerpos de funciones/métodos), firmas por
     tipos (`Symbol.tiposParametros`), built-ins de Y? y el alcance de Y?/Zetariano.
     *Depende de P3/P4 para tener árboles reales de esos lenguajes.*
6. **P6 — Conectar UI**: verificación sintáctica previa para imports (robustez), llenar
   `ventanaErrores` desde semántica, y (con P7) cuartetas/C3D/C.
7. **P7 — Cuartetas**: implementar `traducir` en los nodos + `IntermediateCodeGenerator` +
   reporte `ventanaCuartetas`. *Depende de P5 (semántica) y de P2 (nodos).*
8. **P8 — C3D**: `C3DGenerator.generate` + reporte `ventanaC3D`.
9. **P9 — Código C**: `CCodeGenerator` + menú Ver código C + (decisión) guardar `.c`.
   *Depende de P8.*

Otros pendientes registrados: validar nombre de archivo == clase (Zetariano), Pase A
recursivo a imports‑de‑imports, múltiples constructores con el mismo nombre (hoy
`getTodos()` conserva los duplicados, pero `resolve(nombre)` solo ve el último), y decidir
el C destino de structs/clases.

---

## 17. Convenciones y restricciones que TODO plan debe respetar

1. **Sin comentarios en el código fuente** (nada de `//` ni `/* */` en clases Java), salvo
   los bloques de diseño ya existentes en las gramáticas `.g4`.
2. **Idioma de los nombres:** métodos y campos generales en inglés; las interfaces de
   contexto del pipeline (`CodigoContexto`, `ContextoSemantico`) y los métodos que ya
   existen en español (`analizar`, `traducir`, `construir`, `definir`, `resolver`,
   `entrarAmbito`, `registrarError`, `listarSimbolos`, `tipoDe`) NO se renombran.
3. **Posiciones base 1** para línea y columna en todos los nodos y reportes.
4. **Nunca crear paquetes `<lenguaje>.ast`**: usar siempre `ast.*`. Los únicos paquetes
   por idioma son `grammar`, `astbuilder`, `semantic` (Vocabulary) y `ui` token makers.
5. **Un nodo, una implementación**: cualquier nodo nuevo va al paquete `ast` (o
   `ast/decl`) y los 3 builders lo comparten.
6. **No romper la API existente** sin justificarlo en el plan: `Node`, `ASTVisitor`,
   constructores de nodos, `SymbolTable`, `ContextoSemantico` y `SemanticAnalyzer` son la
   interfaz pública.
7. **Decisiones abiertas** (gaps 7.5, tabla de compatibilidad, C destino, desugar de
   `+=`, etc.) deben decidirse en el plan y quedar documentadas ahí, no en el código.
8. **Resultado de aceptación de cada fase:** `mvn -B clean compile` sin errores + caso
   manual verificado en la GUI (sección 14). No agregar tests automatizados.
9. `mvn -B clean compile` regenera ANTLR: los planes no deben editar los fuentes
   generados en `target/generated-sources/antlr4`.
10. La carpeta de trabajo del build es `Alien_code/` (donde está `pom.xml`).

---

## 18. Notas históricas de arquitectura

- El diseño original triplicaba `ast`, `semantic` y análisis por idioma + una etapa
  "Merge". La corrección (ver sección 2 y el MD en Downloads) lo unificó y ya está
  aplicada al código. **No revertirla.**
- Decisión de alcance fijada en el Pase A: los métodos de una clase **NO** son visibles
  desde el scope global (se resuelven solo a través de `Symbol.getMiembros()`). No cambiar
  esa visibilidad sin justificarlo.

---

## 19. Glosario

| Término | Significado |
|---|---|
| AST | Árbol de sintaxis abstracta, representación común del código fuente |
| ANTLR | Generador de analizadores (lexer + parser) |
| C3D (código de tres direcciones) | Código intermedio con instrucciones de tres operandos |
| Cuarteta | Cuádrupla `(op, a1, a2, res)` de código intermedio |
| Pase A | Pasada de la semántica que registra firmas/declaraciones sin revisar cuerpos |
| Pase B | Pasada que verifica los cuerpos aprovechando el catálogo de Pase A |
| INDENT / DEDENT | Tokens sintéticos que delimitan bloques indentados en Y? |
| TokenMaker | Clase de RSyntaxTextArea para el resaltado de sintaxis |
| Visitor | Patrón de diseño para recorrer el AST sin modificar los nodos |
| Vocabulary | Traducción nombre de tipo → `Type` (una por idioma) |