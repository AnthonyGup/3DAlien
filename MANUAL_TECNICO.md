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
validados por un **único** análisis semántico en **dos pases**. El backend es un único
pipeline compartido: los nodos emiten **cuartetas tipadas** (`c3d.cuartetas.*`) cuyo render
`toCCode` es a la vez C3D y código C final.

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
| Traducción de nombre de tipo → `Type` (en `Type.fromPigLatin`/`fromYLang`/`fromZetariano`) | Generación de cuartetas / C3D / C |

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
                       | Cuartetas tipadas  |
                       |  (c3d.cuartetas.*) |
                       +--------------------+
                                       v
                       +--------------------+
                       | toCCode() +       |
                       | CCodeGenerator →  |
                       | Código C          |
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
│           ├── ylang/astbuilder/YLangASTBuilder.java           # implementado
│           ├── zetariano/astbuilder/ZetarianoASTBuilder.java   # implementado
│           ├── ir/        CodigoContexto, IntermediateCodeGenerator, Operandos,
│           │              Impresion
│           ├── c3d/       TiposC, CodeTransformable,
│           │              access/ (MemoryAccess y 6 accesos),
│           │              cuartetas/ (Cuarteta y 14 cuartetas tipadas)
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
    int getLine();                          // línea base 1
    int getColumn();                        // columna base 1
    Type analizar(ContextoSemantico ctx);   // semántica (34/34 nodos implementados)
}

// La traducción (Fase 4) NO vive en `Node`: la heredan las dos clases base:
public abstract class Expresion implements Node { MemoryAccess traducir(CodigoContexto ctx); }
public abstract class Sentencia implements Node { void traducir(CodigoContexto ctx); }
```

`ProgramNode`, `ImportNode` y todos los nodos de `decl`/`stmt` (incluyendo `FunctionDeclNode`,
`ClassDeclNode`, etc.) extienden `Sentencia`; los `expr` extienden `Expresion`. Único nodo que
implementa `Node` directo sin `traducir`: `ParameterNode`. `Expresion.traducir` devuelve el
acceso C3D (`c3d.access.MemoryAccess`) donde quedó el resultado de la subexpresión.

### 6.2 `Type` (enum compartido)

Valores: `INT, FLOAT, STRING, BOOL, CHAR, VOID, STRUCT, CLASS, ARRAY, NULL`
(`NULL` = literal `null` de Zetariano; ver reglas en `TypeCompat`).

Mapeos estáticos (con `name.toLowerCase()`):
- `fromPigLatin`: `numerus`→INT, `decimalis`→FLOAT, `textum`→STRING, `littera`→CHAR,
  `bool`→BOOL.
- `fromZetariano`: `int`→INT, `double`→FLOAT, `string`→STRING, `boolean`→BOOL,
  `char`→CHAR, `void`→VOID.
- `fromYLang`: `entero`→INT, `flotante`→FLOAT, `cadena`→STRING, `bool`→BOOL,
  `caracter`→CHAR (verificado contra `YLangLexer.g4`; la gramática no tiene `vacio`, así
  que no hay mapeo a `VOID`).

- [ ] **PENDIENTE**: no existe mapeo para `ARRAY` ni para buscar tipos con nombre
      definido en otro archivo (STRUCT/CLASS) — los builders resuelven eso con
      `Type.STRUCT`/`Type.CLASS` + `tipoNombre`/`nombreClase`.

### 6.3 `ASTVisitor` — eliminado

El patrón visitante (`ASTVisitor` + `accept` en cada nodo) se eliminó por no utilizarse:
la semántica y la generación de cuartetas recorren el AST vía `ctx.evaluar(...)` /
`traducir(ctx)`.

---

## 7. Nodos del AST — referencia de campos y constructores

Convención: campos públicos (POJO), línea/columna `final private` con getters. Cada nodo
implementa `traducir` (P7, §9), `analizar` (P5) y `getLine`/`getColumn`; el patrón
visitante fue eliminado (§6.3). `analizar` está implementado en los **34 nodos**
(fases Pig Latin + Y? + Zetariano, ver 8.7–8.8).

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
| `FunctionDeclNode` | `String nombre`, `List<ParameterNode> parametros`, `Type tipoRetorno`, `String tipoRetornoNombre` (null salvo STRUCT/CLASS), `BlockNode cuerpo` | `(nombre, parametros, tipoRetorno, cuerpo, line, col)`; `tipoRetornoNombre` se asigna post-construcción |
| `ClassDeclNode` | `String nombre`, `List<VariableDeclNode> atributos`, `List<ConstructorDeclNode> constructores`, `List<MethodDeclNode> metodos` | `(nombre, atributos, constructores, metodos, line, col)` |
| `ConstructorDeclNode` | `List<ParameterNode> parametros`, `BlockNode cuerpo` | `(parametros, cuerpo, line, col)` |
| `MethodDeclNode` | `String nombre`, `List<ParameterNode> parametros`, `Type tipoRetorno`, `String tipoRetornoNombre`, `Type tipoRetornoElemento` (solo si retorna arreglo), `BlockNode cuerpo` | `(nombre, parametros, tipoRetorno, cuerpo, line, col)`; los dos extras se asignan post-construcción. `analizar` valida `return` contra `tipoRetornoElemento` cuando retorna arreglo |
| `ParameterNode` | `Type tipo`, `String nombre`, `boolean porReferencia`, `String tipoNombre`, `Type tipoElemento` (solo arreglo), `int dimensiones` (1 por defecto) | `(tipo, nombre, porReferencia, tipoNombre, line, col)` (`tipoNombre` = nombre del tipo si es STRUCT/CLASS); `tipoElemento`/`dimensiones` se asignan post-construcción |

`tipoRetorno == null` significa "sin retorno" (Y? sin `->`, método `void`); el Pase A lo
guarda tal cual y `ReturnNode` lo interpreta como "no debe retornar valor".

### 7.3 `expr`

| Clase | Campos públicos | Constructor / notas |
|---|---|---|
| `AccessNode` | `String nombre`, `List<Sufijo> sufijos` | `(nombre, line, col)`; método `conSufijos()`. `Sufijo` interno: `enum Tipo {CAMPO, INDICE, LLAMADA}`, campos `tipo`, `nombreCampo`, `indice` (Node), `argumentos` (List<Node>); factories estáticas `Sufijo.campo(name)`, `Sufijo.indice(node)`, `Sufijo.llamada(list)`. Estáticos de apoyo: `tiposDeArgumentos(ctx, args)` (evalúa y mapea accesos a símbolos arreglo → `ARRAY`, usado también por `NewObjectNode`) y `resolverSimbolo(ctx, acceso)` (resuelve el símbolo final sin reportar errores; `null` si hay llamada o algo falla) |
| `BinaryOpNode` | `String operador`, `Node izquierda`, `Node derecha` | `(operador, izquierda, derecha, line, col)`. `operador` guarda el texto crudo: `||`, `&&`, `==`, `!=`, `<`, `>`, `<=`, `>=`, `+`, `-`, `*`, `/`, `%` |
| `UnaryOpNode` | `String operador`, `Node operando` | `(operador, operando, line, col)`. `operador`: `non`, `!` o `-` |
| `LiteralNode` | `enum Clase {ENTERO, DECIMAL, CADENA, CARACTER, BOOLEANO, NULO}`, `Clase clase`, `String valor` | `(clase, valor, line, col)`. `valor` es el texto crudo (incluye comillas para CADENA/CARACTER si así lo emite el builder). `NULO` → `Type.NULL` |
| `NewObjectNode` | `String nombreClase`, `List<Node> argumentos` | `(nombreClase, argumentos, line, col)`. Instancia de objeto (`new Clase(...)` / `novus Clase(...)`); valida contra `firmasConstructores` del `Symbol` |
| `StructLiteralNode` | `List<Node> valores` | `(valores, line, col)`. Literal `{...}` posicional (estructura o arreglo según contexto) |
| `ConditionalNode` | `Node condicion`, `Node siVerdadero`, `Node siFalso` | `(condicion, siVerdadero, siFalso, line, col)`. Ternario `a ? b : c` de Zetariano |
| `NewArrayNode` | `Type tipoElemento`, `String tipoNombreElemento` (null si primitivo), `List<Node> dimensiones` | `(tipoElemento, dimensiones, line, col)` o `(tipoElemento, tipoNombreElemento, dimensiones, line, col)`. `new int[5]`, `new int[3][3]`, `new Clase[n]` |

### 7.4 `stmt`

| Clase | Campos públicos | Constructor |
|---|---|---|
| `VariableDeclNode` | `String nombre`, `Type tipo`, `String tipoNombre`, `Type tipoElemento` (null salvo arreglo), `int dimensiones` (1 por defecto), `Node inicial` | `(nombre, tipo, tipoNombre, inicial, line, col)`. Declaración estilo Zetariano (`int[] x = ...`) usa `tipo=ARRAY` + `tipoElemento` + `dimensiones`; si `inicial` es `NewArrayNode`/`StructLiteralNode` se valida contra el elemento; si es acceso a otro arreglo (`int[] b = a`) se acepta y se valida el elemento |
| `ArrayDeclNode` | `String nombre`, `int tamano`, `int dimensiones` (1 por defecto), `Type tipo`, `String tipoNombre`, `List<Node> iniciales` | `(nombre, tamano, tipo, tipoNombre, iniciales, line, col)` |
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
| `ReturnNode` | `Node expresion` (null si no retorna nada) | `(expresion, line, col)`. Valida contra `ctx.tipoRetornoActual()` |
| `SwitchNode` | `Node expresion`, `List<CaseNode> casos` | `(expresion, casos, line, col)`. Activa `entrarSwitch` para sus casos |
| `CaseNode` | `Node valor` (null = `siempre`/`default`), `List<Node> sentencias` | `(valor, sentencias, line, col)`; `esSiempre() = valor==null` |

NOTA `tipoNombre`: en `VariableDeclNode`/`ArrayDeclNode`/`ParameterNode` y builders de Pig
Latin se usa cuando el tipo es un identificador (estructura/clase): `tipo=Type.STRUCT` y
`tipoNombre="Persona"`. Así se conserva el nombre original para resolver por nombre en
semántica (la tabla se indexa por nombre).

### 7.5 GAPS del AST frente a las gramáticas (CRITICO para planear fases)

La gramática de Zetariano/Y? acepta construcciones que **el AST aún no puede representar**.
Cada gap requiere decidir un nodo nuevo o un desglose ("desugar") dentro del builder. Lista:

| # | Feature (de qué gramática) | Gap | Opciones a decidir |
|---|---|---|---|
| 1 | `retornar expr` (Y?), `return expr?` (Z) | **RESUELTO**: `stmt/ReturnNode.java` + mapeo en ambos builders | — |
| 2 | `elegir`/`caso`/`siempre` (Y?), `switch`/`case`/`default` (Z) | **RESUELTO**: `stmt/SwitchNode.java` + `stmt/CaseNode.java` + mapeo en ambos builders; `BreakNode` acepta `enSwitch()` | — |
| 3 | Ternario `a ? b : c` (Z) | **RESUELTO**: `expr/ConditionalNode.java` + mapeo (`expresionOr (INTERROGACION ...)?`) | — |
| 4 | `null` (Z) | **RESUELTO**: `Clase.NULO` + `Type.NULL` + reglas en `TypeCompat` + mapeo | — |
| 5 | `new int[5]`, `new int[3][3]`, `new Clase[]` (Z) | **RESUELTO**: `expr/NewArrayNode.java` (`tipoElemento` + `tipoNombreElemento` opcional + `dimensiones`) + mapeo | — |
| 6 | Asignación compuesta `+=` `-=` `*=` (Z) | **RESUELTO**: desugar en `ZetarianoASTBuilder.visitAsignacionCompuesta` (`destino = destino op valor`, comparte la lista de sufijos) | — |
| 7 | Arreglos multidimensionales `entero m[3][3]` (Y), `int[][]` (Z) | **RESUELTO**: modelo `tipoElemento` + `dimensiones` en `VariableDeclNode`/`ArrayDeclNode`/`ParameterNode`/`Symbol`; declaraciones estilo Zetariano (`tipo arrayDims? IDENTIFICADOR (= expr)?`) van a `VariableDeclNode(tipo=ARRAY, ...)`; `ArrayDeclNode` queda para estilo Y?/Pig Latin (`name[N]`, `tamano` = producto de dims) | — |
| 8 | Caracteres/arreglos en **campos de estructura y parámetros** (Y: `tipoCampo IDENTIFICADOR ([N])*`; Z: `int[] arr`) | **RESUELTO**: campos vía `VariableDeclNode` (con dims); parámetros vía `ParameterNode.tipoElemento`+`dimensiones` | — |
| 9 | Cuerpo sin llaves de `if`/`for`/`while`/`do` (Z) | **RESUELTO**: `cuerpoOSentencia` envuelve la sentencia única en `BlockNode` de 1 elemento | — |
| 10 | `imprimir(...)`/`leer()` (Y), `println` (Z) | **RESUELTO**: símbolos nativos (`SemanticAnalyzer.registrarNativas`, `Symbol.isNativa`, rama en `AccessNode` que retorna el tipo sin validar args) | — |
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

    // Adiciones de las fases semánticas (no rompen el constructor ni las llamadas existentes):
    public String getTipoNombre(); public void setTipoNombre(String tipoNombre);
    public Scope getMiembros();   public void setMiembros(Scope miembros);
    public java.util.List<Type> getTiposParametros();       // firma única (FUNCION de Y?)
    public void agregarFirma(java.util.List<Type> tipos);   // una por sobrecarga (METODO)
    public void agregarFirmaConstructor(java.util.List<Type> tipos);  // (CLASE)
    public boolean tieneFirmaCompatible(java.util.List<Type> argumentos);
    public boolean tieneConstructorCompatible(java.util.List<Type> argumentos);
    public boolean isNativa(); public void setNativa(boolean nativa); // built-ins (imprimir/leer/println)
    public int getLinea(); public void setLinea(int linea);           // posición para el reporte UI
    public int getColumna(); public void setColumna(int columna);
    public int getDimensiones(); public void setDimensiones(int d);   // nº de dims (arreglos)
    public static Symbol variable(String nombre, Type tipoDeclarado, Type tipoElemento,
        String tipoNombre, int dimensiones, boolean isParameter, boolean isField,
        int line, int column);   // factory: si es ARRAY guarda el ELEMENTO en `type`
}
```

`tipoNombre` guarda el nombre real cuando `type` es `STRUCT`/`CLASS` (si no, `null`).
`miembros` es el `Scope` con los campos/métodos, adjuntado por
`SemanticAnalyzer.paseA` solo a los símbolos `ESTRUCTURA`/`CLASE`.
`tiposParametros` guarda la firma única de una `FUNCION`;
`firmas`/`firmasConstructores` guardan las N firmas de un `METODO` sobrecargado y los
constructores de una `CLASE` (los constructores **no** viven en `miembros` porque se
acceden con `new`, no con `.`). Todas las sobrecargas de un método comparten el `type`
(retorno): solo varía la lista de parámetros. La comparación usa
`TypeCompat.esAsignable` por posición (con `null` = comodín por error ya reportado).

**Modelo de arreglos:** un símbolo arreglo guarda el tipo del **elemento** en `type`,
`isArray=true` y el nº de dimensiones en `dimensiones` (0 = no es arreglo). Por eso una
variable arreglo **evalúa** a su tipo elemento (`ctx.evaluar(arr)` → `INT` para `int[]`),
y `Type.ARRAY` solo aparece como tipo declarado (`VariableDeclNode.tipo`), resultado de
`NewArrayNode`/`NewArrayNode`-como-argumento, o retorno declarado de arreglo. Al validar
llamadas, `AccessNode.tiposDeArgumentos` mapea accesos a símbolos arreglo → `ARRAY`
(ver 7.3). Limitación menor: `Symbol.variable()` fija `size=0`, así que los arreglos
declarados vía `VariableDeclNode` (campos Y?, estilo Zetariano) no conservan el tamaño
declarado (solo `ArrayDeclNode` lo guarda en `size`).

### 8.2 `Scope`

Ámbito con padre y `Map<String,Symbol>` (`LinkedHashMap`). `define` inserta en el mapa y
además agrega a una lista `orden`, de modo que `getTodos()` devuelve los símbolos **en
orden de declaración** y **conserva los duplicados** por nombre (necesario para los
constructores, que comparten el nombre de la clase). `getTodos()` es la base de
`StructLiteralNode.validarContra`. `resolveLocal(name)` busca **solo** en el ámbito
propio (sin subir al padre): se usa en `registrarClase` para distinguir sobrecarga
(agregar firma) de método nuevo (crear `Symbol`).

### 8.3 `SymbolTable`

Pila de `Scope` (en el constructor ya entra el ámbito global) + `registro` permanente
(con cada `Symbol` definido, aunque su ámbito ya se haya cerrado). API:

- `enterScope()`, `exitScope()` (no desapila el último: el global nunca se cierra).
- `enterScopeWithin(Scope padre)` (el nuevo ámbito cuelga del padre dado, no del activo).
- `ambitoActual()` (el `Scope` en el tope, para capturarlo antes de salir de él).
- `isDefinedInCurrentScope(name)`, `define(symbol)` (además agrega a `registro`),
  `resolve(name)` (del ámbito actual hacia afuera).
- `listarSimbolos()` → `List<Symbol>` deduplicada por nombre (recorre `registro`),
  usada por el reporte de la tabla de símbolos de la UI. Incluye locales, parámetros y
  nativas; las sobrecargas aparecen una sola vez (comparten `Symbol`).

### 8.4 `ContextoSemantico` (interfaz) y `ContextoSemanticoImpl`

```java
public interface ContextoSemantico {
    Type evaluar(Node nodo);            // nodo.analizar(this)
    void entrarAmbito();  void salirAmbito();
    void entrarAmbitoDentroDe(Scope padre);   // el nuevo ámbito cuelga de `padre`
    Scope ambitoActual();
    boolean definir(Symbol simbolo);    // false si ya existe en el ámbito actual
    Symbol resolver(String nombre);
    void registrarError(int linea, int columna, String mensaje);   // ErrorType.SEMANTICO
    void entrarCiclo();  void salirCiclo();  boolean enCiclo();     // para romper/continuar
    void entrarSwitch(); void salirSwitch(); boolean enSwitch();   // `romper` vale en elegir aunque no haya ciclo
    void pushTipoRetorno(Type t); void popTipoRetorno(); Type tipoRetornoActual();  // pila (t=null: sin retorno)
    void pushClaseActual(Scope miembros);       // pila (clases anidadas)
    void popClaseActual();
    Scope ambitoDeClaseActual();
}
```

`ContextoSemanticoImpl` implementa todo sobre `SymbolTable` + `ErrorListener`, con
contadores `loopDepth`/`switchDepth`, una pila de retornos (`LinkedList`, admite `null`)
y una pila de ámbitos de clase. También expone `getSymbolTable()`.
`entrarAmbitoDentroDe` permite que el cuerpo de un método/constructor "cuelgue" del
ámbito de miembros de su clase: así `nombre = x;` resuelve el atributo **sin** `this.`.

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

`analizar(programas)` = `paseA` + `registrarNativas` + `paseB`.
`registrarNativas`: si algún programa es `"Y?"`, define `imprimir` (VOID) y `leer`
(type `null`) como `FUNCION` nativas; si alguno es `"Zetariano"`, define `println`
(VOID) nativa. `AccessNode` retorna el tipo de una nativa sin validar argumentos (pero
sí los evalúa, para que sus errores internos afloren). Si el usuario declara su propia
función con el mismo nombre, la suya gana (el `definir` de la nativa falla en silencio).

`paseA` → para cada `programa.declarations`, según su tipo (campos/atributos siempre vía
`Symbol.variable`, que guarda elemento+dims+línea):
- `registrarEstructura(StructDeclNode)`: `entrarAmbito`, define cada campo
  (error "Campo duplicado" si se repite), captura `ambitoActual`,
  `salirAmbito`, define `(nombre, STRUCT, ESTRUCTURA, size=campos.size)` con
  `setMiembros(scope)` + línea/columna (error "ya fue declarada" si se repite).
- `registrarFuncion(FunctionDeclNode)`: define `(nombre, tipoRetorno, FUNCION,
  size=parametros.size)` guardando `tiposParametros` en el `Symbol` (+ `tipoNombre` si
  retorna STRUCT/CLASS y línea/columna; error "ya fue declarada" si se repite).
- `registrarClase(ClassDeclNode)`: `entrarAmbito`, define atributos (error "Atributo
  duplicado"); por cada método busca `resolveLocal(nombre)`: si no existe crea el
  `Symbol METODO` con su primera firma, si existe le agrega la firma (sobrecarga);
  captura `ambitoActual`, `salirAmbito`, define `(nombre, CLASS, CLASE,
  size=atributos.size)` con `setMiembros(scope)`, línea/columna y
  `agregarFirmaConstructor` por cada constructor (los constructores **no** van a
  `miembros`).

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
- **Hecho (fase semántica Y?, spec `Semantico_YLang.md`):**
  - `Symbol` + `tiposParametros`/`porReferencia`; `ParameterNode` + `tipoNombre`.
  - `ContextoSemantico`: `ambitoActual`, pila de retornos
    (`pushTipoRetorno`/`popTipoRetorno`/`tipoRetornoActual`), `entrarSwitch`/`salirSwitch`/
    `enSwitch`.
  - Nodos nuevos `ReturnNode`, `SwitchNode`, `CaseNode` (con `analizar` y visitor).
  - `registrarEstructura`/`registrarFuncion` en Pase A (errores de duplicados + firmas).
  - `analizar` en `StructDeclNode` (valida campos STRUCT contra la tabla),
    `ParameterNode` (solo ARRAY/STRUCT/CLASS por referencia + define la local) y
    `FunctionDeclNode` (ámbito + parámetros + cuerpo con retorno esperado).
  - `BreakNode` acepta `enSwitch()`; `ContinueNode` sigue exigiendo solo ciclo.
  - `AccessNode` (rama LLAMADA a función) valida cantidad y tipo de argumentos contra
    `tiposParametros` con `esAsignable`.
- **Hecho (fase semántica Zetariano, spec `Semantico_Zetariano.md`):**
  - `Type.NULL` + `LiteralNode.Clase.NULO` + reglas en `TypeCompat` (`null` asignable a
    STRUCT/CLASS y comparable con `==`/`!=` contra ellos; cubre `if(p1 == null)`).
  - `Symbol` + `firmas`/`firmasPorReferencia`/`firmasConstructores` con
    `tieneFirmaCompatible`/`tieneConstructorCompatible`; `Scope.resolveLocal`.
  - `ContextoSemantico`: `entrarAmbitoDentroDe`, pila de clase actual
    (`pushClaseActual`/`popClaseActual`/`ambitoDeClaseActual`).
  - `registrarClase` en Pase A (atributos + métodos con sobrecarga + firmas de
    constructores).
  - `analizar` en `ClassDeclNode` (valida atributos STRUCT/CLASS + evalúa
    constructores/métodos con la clase actual), `ConstructorDeclNode` y
    `MethodDeclNode` (cuerpo colgado de los miembros: atributos visibles sin `this.`).
  - `NewObjectNode` valida contra `firmasConstructores`; `AccessNode` (rama LLAMADA a
    método) valida contra `tieneFirmaCompatible`.
  - Nodos nuevos `ConditionalNode` (ternario) y `NewArrayNode` (con `analizar` y visitor);
    `BinaryOpNode` soporta `%`.
  - Decisión de arreglos (§2.3 del spec): Zetariano declara `tipo arrayDims?
    IDENTIFICADOR (= expr)?` (corchetes en el tipo, sin tamaño) → `ZetarianoASTBuilder`
    lo mapea a `VariableDeclNode(tipo=ARRAY, tipoElemento=..., dimensiones=N)`;
    `ArrayDeclNode` queda para estilo Y?/Pig Latin (`name[N]`). `NewArrayNode` lleva
    `tipoNombreElemento` opcional para `new Clase[n]`.
- **Completado (verificado con `ejemplo.y`, `Persona.z` y pruebas negativas):**
  - `YLangASTBuilder`/`ZetarianoASTBuilder` implementados: emiten árboles reales de
    `.y`/`.z` (`retornar`/`return`, `elegir`/`switch`, ternario, `null`, `new` de
    arreglos, dims y multidim, cuerpos sin llaves, `%`, `+=`/`-=`/`*=` en Z).
  - `Type.fromYLang` verificado contra `YLangLexer.g4`
    (`entero`/`flotante`/`cadena`/`bool`/`caracter`; no existe `vacio` en la gramática).
  - Built-ins `imprimir`/`leer` (Y?) y `println` (Z) registrados como símbolos nativos
    (`SemanticAnalyzer.registrarNativas`, rama `isNativa` en `AccessNode`).
- **Correcciones de alineación (post-revisión):**
  - Los 3 builders recorren `ctx.children` en orden al armar `BinaryOpNode` en
    igualdad/relacional/aditiva/multiplicativa (antes agrupaban por tipo de operador y
    `a - b + c` se armaba mal).
  - Arreglos como valor: `int[] b = a` aceptado (con chequeo de elemento);
    `f(arr)`/`new P(arr)` aceptados para parámetros `ARRAY` (`tiposDeArgumentos` mapea
    accesos a símbolos arreglo); literal `{...}` aceptado para `ARRAY` en llamadas a
    `FUNCION`.
  - `ParameterNode.dimensiones` (params `int[][]` conservan la cuenta).
  - `UnaryOpNode` acepta `"!"` además de `"non"`; los builders Y/Z pasan el texto crudo.
- **Pendiente (menor):**
  - `Symbol.variable()` fija `size=0`: sin chequeo de conteo al asignar literales a
    arreglos declarados vía `VariableDeclNode`.
  - Llamadas con literal `{...}` a parámetros de sobrecargas/métodos/constructores no
    validan elementos; `StructDeclNode.analizar` solo valida campos `STRUCT` (no `CLASS`).
  - Colisión de nombre entre función de usuario y nativa: gana la del usuario en silencio.
- La tabla de compatibilidad de tipos vive en `semantic/TypeCompat`: único
  ensanchamiento `INT`→`FLOAT`; `+` sobre `STRING` concatena; comparación `==`/`!=`
  admite iguales, numéricos o `NULL` contra STRUCT/CLASS; `<`/`>` solo numéricos.

### 8.8 Nodos con `analizar()` implementado (34/34: fases Pig Latin + Y? + Zetariano)

| Grupo | Nodos |
|---|---|
| Raíz | `ProgramNode`, `ImportNode`, `BlockNode` |
| Declaraciones | `VariableDeclNode`, `ArrayDeclNode`, `StructLiteralNode` (+ `validarContra(Symbol,ContextoSemantico)`), `StructDeclNode`, `FunctionDeclNode`, `ClassDeclNode`, `ConstructorDeclNode`, `MethodDeclNode`, `ParameterNode` |
| Expresiones | `LiteralNode`, `BinaryOpNode`, `UnaryOpNode`, `AccessNode`, `NewObjectNode`, `ConditionalNode`, `NewArrayNode` |
| Sentencias | `AssignmentNode`, `IncrementNode`, `DecrementNode`, `IfNode`, `ElseIfNode`, `WhileNode`, `DoWhileNode`, `ForNode`, `BreakNode`, `ContinueNode`, `PrintNode`, `ReadNode`, `ReturnNode`, `SwitchNode`, `CaseNode` |

Patrón general por nodo: se documenta el tipo declarado del destino/símbolo, se
resuelven/reportan errores y se registra el símbolo con `ctx.definir`. Para conocer el tipo
de un hijo se usa `ctx.evaluar(hijo)` (equivale a `hijo.analizar(ctx)`).

**Limitaciones conocidas (seguir en fases posteriores):** las llamadas a
función/método validan cantidad y tipo pero no el `tipoNombre` de STRUCT/CLASS (la
comparación usa `Type` plano + `esAsignable`); un acceso a símbolo arreglo como
argumento se mapea a `ARRAY` (`tiposDeArgumentos`), pero un literal `{...}` solo se
acepta para `ARRAY` en la rama `FUNCION` (sobrecargas/métodos/constructores no lo
contemplan); `ReturnNode` fuera de función se trata como "sin retorno esperado"; la
compatibilidad de `STRUCT`/`CLASS` que llega como resultado de un `AccessNode` compara
solo el `Type` (no el nombre), porque `Type` es un enum plano.

---

## 9. Gramáticas ANTLR — notas de diseño relevantes para planes

### 9.1 PigLatin.g4 (terminada)

- Marcadores `VARIABILES>`/`MAIOR>` son tokens propios (`VARIABILES_MARKER`,
  `MAIOR_MARKER`). `FINIS` (mayúsc.) y `finis` son tokens distintos.
- Todo el caso de bloques se maneja con `cuerpoBloque = { ... }`. `per` deja el
  `finis;` final opcional.
- `imprimir : ESCRIBIR (ESCRIBIR? expresion)+ PUNTO_COMA ;` (`>> "a" >> x ;`).
- Comentarios `//` y `##...##` en canal `HIDDEN`.

### 9.2 YLangLexer.g4 + YLangParser.g4 (terminada)

- **Lexer de solo lexer** (`lexer grammar`) que declara `tokens { INDENT, DEDENT }` y
  sobrescribe `nextToken()` con: pila de indentación (`indentStack`), `opened` (no genera
  INDENT/DEDENT dentro de `(`/`[`), cola de tokens (`tokenQueue`), y cierre de niveles en
  `EOF` (`eofHandled`). La separación lexer/parser es obligatoria: en una gramática
  combinada los tokens sintéticos no existen en el código Java del lexer. Los bloques son
  `NEWLINE INDENT sentencia+ DEDENT`.
- `NEWLINE` salta líneas en blanco/comentarios y captura la indentación final de la línea.
- Operadores relacionales de Y? son SOLO `<` `>` (no hay `MENOR_IGUAL`/`MAYOR_IGUAL`).
- `imprimir`/`leer` NO son tokens: se parsean como `accesoVariable` con sufijo de llamada.

### 9.3 Zetariano.g4 (terminada; es la gramática real de los alumnos)

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

### 10.2 `YLangASTBuilder` (IMPLEMENTADO)

Extiende `YLangParserBaseVisitor<Node>` y traduce `structura` → `StructDeclNode`,
`funcion` → `FunctionDeclNode` (+ `ParameterNode` con `porReferencia = []|{}` y
`tipoNombre`), cuerpo de función → `BlockNode`, y las sentencias Y? a nodos
`stmt`/`expr` (return, switch, dims en campos/arreglos incl. multidim con `tamano` =
producto, etc.). `sourceLanguage = "Y?"`. Las reglas de igualdad/relacional/aditiva/
multiplicativa recorren `children` en orden (asociatividad izquierda correcta);
`!` se pasa crudo a `UnaryOpNode`.

### 10.3 `ZetarianoASTBuilder` (IMPLEMENTADO)

Traduce `programa` (una clase) → `ClassDeclNode` (atributos, constructores, métodos);
campos → `VariableDeclNode` (arreglos con `tipoElemento`+`dimensiones`);
métodos/constructores → `MethodDeclNode`/`ConstructorDeclNode` con `BlockNode`
(+ `tipoRetornoNombre`/`tipoRetornoElemento`); sentencias/expresiones → `stmt`/`expr`
(ternario, `null`, `new int[]`/`new Clase[]`, `+=` con desugar que comparte sufijos,
`return`, `switch`, dims, cuerpos sin llaves vía `cuerpoOSentencia`, `%`).
`sourceLanguage = "Zetariano"`. Para `if` anidado con `else if`: `ramaElse→condicional`
se colapsa a `ElseIfNode` (método `aplanarRamaElse`). Igual que Y?: `children` en orden
y `!` crudo.

---

## 11. Referencia del backend (`ir`, `c3d`, `codegen`)

El backend genera **cuartetas tipadas** (`c3d.cuartetas.*` reutilizando accesos de
`c3d.access`) y su render a C es a la vez el "C3D" y el código C final: NO existe un pase
intermedio `C3DGenerator` (fue eliminado en la Fase 5).

### 11.1 `ir.CodigoContexto` (contrato de emisión)

```java
public interface CodigoContexto {
    int nuevoIndiceTemporal();                    // reserva el nº de temporales
    String nuevaEtiqueta();                       // "L0", "L1", ...
    void agregar(Cuarteta cuarteta);              // cuartetas TIPADAS (c3d.cuartetas.*)
    void empujarCiclo(String etiquetaContinuar, String etiquetaSalida);
    void popCiclo();
    String etiquetaContinuarActual();             // null si no hay ciclo
    String etiquetaSalidaActual();
    void pushClassTranslation(String nombre);     // pila de clase (sufijo _i)
    void popClassTranslation();
    String currentClassName();
    int nextMethodIndex(String methodName);
    int nextConstructorIndex();
    void registrarTipoTemporal(int temporal, String ctype);  // tipo C de cada temporal
    String tipoDeTemporal(int temporal);
    Symbol resolverSimbolo(String nombre);        // mapa plano sobre la tabla
    SymbolTable getSymbolTable();
}
```

### 11.2 `ir.IntermediateCodeGenerator` (implementa `CodigoContexto`)

Campos: `List<Cuarteta> instrucciones` (cuartetas tipadas), `Map<Integer,String>
tiposTemporales`, `Deque<String[]> ciclos`, `tempCounter`, `labelCounter`, pila de clases y
un mapa plano `simbolosPlano` construido de forma lazy desde `tabla.listarSimbolos()`
(first-wins; la tabla puede llegar vacía si se emite sin semántica previa). API:
`getInstrucciones()`, `getTiposTemporales()`, constructores `(ErrorListener)` y
`(ErrorListener, SymbolTable)`, `resolverSimbolo(nombre)`, pila de clases (`_i` de
métodos/constructores: `pushClassTranslation`/`popClassTranslation`, `currentClassName`,
`nextMethodIndex`/`nextConstructorIndex`) y `overloadIndexByArgCount`. **Los nodos emiten
con `agregar(...)`**: no hay `emitir`/`nuevoTemporal` de String. El factory de accesos es
`ir.Operandos` (`temporal(ctx,ctype)` — registra además el tipo C, `etiqueta`, `nombre`,
`tipoDe`, `ctypeDe`, `literalBraces`, `ejecutar`) y el de texto de impresión es
`ir.Impresion.print`. **HECHO (Fase 4)**: los 34 nodos implementan `traducir(ctx)`; los
temporales de expresión/escondite son `tN` con tipo C registrado en `registrarTipoTemporal`.

### 11.3 Accesos y cuartetas tipadas (`c3d`)

**`ir.Cuarteta`, `c3d.C3DInstruction` y `c3d.C3DGenerator` fueron ELIMINADOS (Fase 5).**
En su lugar:

- `c3d.CodeTransformable`: `void toCCode(StringBuilder sb);`
- `c3d.TiposC`: mapa de tipos Alien → C (`INT/BOOL→int`, `FLOAT→double`, `CHAR→char`,
  `STRING→char*`, `STRUCT/CLASS→struct X *`, `ARRAY→void*` con retorno `void*` como
  limitación documentada).
- `c3d.access.MemoryAccess` (abstracta): tipo/dims (`getTipoNombre`, `getDimensiones`) y
  `toCCode(sb)`. Subclases: `NameAccess` (`x`, y `self->x` si es campo de la clase actual),
  `CampoAccess` (`base->campo`), `IndiceAccess` (`base[i]`/`base[i][j]`),
  `TemporalAccess` (`tN`), `Literal3D` (texto crudo) y `LabelAccess` (`L0`).
- `c3d.cuartetas.Cuarteta` (abstracta): `operator()`, `getOperand1/getOperand2/getResult()`
  (operandos `String`, resultado `MemoryAccess`), `toCCode(sb)` y `static render(MemoryAccess)`.
  Subclases: `Asignacion3D`, `Operacion3D`, `Condicional3D`, `Goto3D`, `Etiqueta3D`,
  `Llamada3D`, `Imprimir3D`, `Leer3D`, `Retornar3D`, `InicioFuncion3D`,
  `FinFuncion3D`, `DeclararArreglo3D`, `Marcador3D` y `Halt3D`.

### 11.4 Render C3D/C por cuarteta (una sola fuente de verdad)

El texto que muestra "Ver C3D" y el que alimenta al `.c` es el **mismo**:
`cuarteta.toCCode(sb)`. Detalles:

- Accesos: `self->campo` (campo de la clase actual en Pig Latin/Zetariano), `a->b`
  (`CampoAccess`), `a[i]`/`a[i][j]` (índices), `tN` (temporales), literales tal cual, `L0`.
- `Operacion3D`: `res = a op b;` (binaria), `res = op a;` (unaria), `res = a;` (sin destino),
  y `res = conc(a, b);` para `conc`/`strn`/`strd` (concatenación).
- `Condicional3D`: `if (!(c)) goto L;` (verdadero=false) / `if (c) goto L;` (verdadero=true).
- `Llamada3D`: `[res =] f(a, b);`.
- `Imprimir3D`: un solo `printf("fmt", args);`; formato por tipo del argumento (`%d` int,
  `%g` double, `%s` string, booleano como ternario `e ? "true" : "false"`, `%c` char, `%p`
  struct). El texto y la lista de argumentos ya los arma `ir.Impresion.print` (distingue
  literales de texto de argumentos). `Leer3D`: `scanf("…", [&]obj);` con `&` para escalares
  (el destino, por ejemplo un campo `&self->x`)
- `Retornar3D`: `return x;` o `return;`; `Halt3D`: `exit(0);`.
- Marcadores: `InicioFuncion3D` (`// func nombre (descripcion)`, con getters `getNombre/
  getDescripcion/getClase/getRetorno`), `FinFuncion3D` (`// func_end …`) y
  `DeclararArreglo3D` (`// array nombre (dims)`); en la vista C3D se muestran como
  comentarios y `CCodeGenerator` los usa para partir el código (no se escriben al `.c`).

### 11.5 `codegen.CCodeGenerator`

**HECHO (Fase 5)** — `generate(List<Cuarteta>, SymbolTable, Map<Integer,String>
tiposTemporales)` ensambla el `.c` completo (devuelve el texto desde `generate`; no hay
`getCode()`):

1. Cabeceras `#include <stdio.h>`/`<stdlib.h>` (+`<string.h>` si hay `conc/strn/strd`) y
   helper `conc` (malloc + memcpy) cuando alguna cuarteta usa concatenación (detectada por
   `Llamada3D.getFuncion()` o el operador de `Operacion3D`).
2. `typedef` forward y definición de cada estructura/clase
   (`Symbol.Kind.ESTRUCTURA/CLASE`) con sus campos (`isField`) mapeados a C; campo-arreglo →
   `elem *` (arreglo de struct → `struct X *` el elemento).
3. Arreglos fijos globales (`DeclararArreglo3D` fuera de función → `scalarDePalabra + nombre
   + dims`, p.ej. `int m[2][3];`) y variables globales escalares (`tabla.resolve(nombre)` no
   nulo, excluye campos y arreglos).
4. Prototipos y cuerpos: la firma sale de `InicioFuncion3D` (`getRetorno()`, y el descriptor
   `self:Clase; a:int; ...` → parámetros). En cada cuerpo se declaran sus arreglos fijos,
   temporales (`tN` → `tiposTemporales`, con fallback `int`) y locales (símbolos VARIABLE
   no-parámetro no-campo no-global) recolectando los tokens del texto render (los nombres
   tras `->`/`.` no se declaran).
5. `main`: si hay `Halt3D` (Pig Latin, fin del `MAIOR>` proyectado en Pig Latin) → cuerpo
   con las cuartetas de nivel 0 + `return 0;`; si no → `int main(void) { return 0; }`.

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

1. `log.limpiar()`, `ventanaErrores.limpiar()`, `ventanaSimbolos.limpiar()`.
2. Editor activo → `VerificadorSintactico.verificar(texto, archivo)`.
3. Si extensión no válida → error y fin.
4. Log tokens + "Análisis sintáctico...". Si hay errores sintácticos → filas en
   `ventanaErrores` + cada error también al log (`[Sintáctico] Línea X, Columna Y: …`)
   + resumen en log, y fin.
5. Sin errores sintácticos: si el archivo es `.pig`/`.y`/`.z`,
   `ejecutarPipelineSemantico(archivo, log, ventanaErrores)`:
   - `construirAST(archivo)` según extensión (si `null` → error).
   - Si es `.z`: verifica que el archivo se llame igual que la clase pública
     (`nombreClasePrincipal`) → fila de error semántico si no.
   - Por cada `ImportNode` en `programa.declarations`: `resolverImport(rutaCompleta)`
     (`carpetaProyecto/ruta(.y|.z)`); si no existe → fila de error semántico con la
     posición del import; si el builder devuelve `null` → pendiente en log.
   - `SemanticAnalyzer.analizar(programas)` → errores semánticos a filas de
     `ventanaErrores`.
   - Si hay filas de error → cada error también al log (`[Semántico] Línea X, Columna Y: …`)
      + resumen en log y fin (no se muestran símbolos).
- Si no: "Análisis semántico completado." + `mostrarSimbolos(...)` → `ventanaSimbolos`
      (`Tipo` muestra `"vacio"` si es `null` como `leer`; `Valor` siempre `"-"`;
      `Línea` desde `Symbol.getLinea()` o `"-"`).
   - P7: `ventanaCuartetas.limpiar()`, `new IntermediateCodeGenerator(errores,
      analizador.getSymbolTable())` y `p.traducir(generador)` por
      cada programa (incluidos los importados); cada cuarteta → fila
      `{#incremental, cuarteta.operator(), textoDe(cuarteta.getOperand1()),
       textoDe(cuarteta.getOperand2()), textoDe(cuarteta.getResult())}` (helper
      `textoDe(MemoryAccess)` = `null`→`"-"`, si no `toCCode`) con resumen
      "Cuartetas generadas: N".
   - P8/P9 (unificados en la Fase 5, ya no hay `C3DGenerator`): `ventanaC3D.limpiar()`
      y se llena con el **mismo** texto `toCCode` de cada cuarteta de
      `generador.getInstrucciones()` → fila `{#incremental, texto}`; `codigoCActual` =
      `new CCodeGenerator().generate(instrucciones, analizador.getSymbolTable(),
      generador.getTiposTemporales())`; log "C3D generado (N)" + "Código C generado".
6. Otra extensión → log "pendiente backend".

- [x] **HECHO (P7)**: cuartetas a `ventanaCuartetas`.
- [x] **HECHO (P8/P9, Fase 5)**: C3D a `ventanaC3D` y código C en `codigoCActual`; ambos
      salen del mismo render `toCCode` de `getInstrucciones()`; "Ver código C"
      abre `VentanaCodigoC` (JTextArea en fuente `Font.MONOSPACED`, botón "Guardar..."
      con `JFileChooser` por defecto `codigo.c`); si no hay código aún → log pendiente
      "Compila un archivo primero".
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

Carpeta `Ejemplos/` (en la raíz): `ejemplo.pig`, `ejemplo.y`, `Persona.z` (el `.z` se
llama igual que su clase, regla del spec, verificada por la GUI). Cubren casi todo cada
gramática y están verificados: 0 errores sintácticos en los 3 y 0 errores semánticos en
los 3 (pipeline completo en cada lenguaje). Abrir esa carpeta como proyecto en la GUI
sirve como prueba manual de regresión. **Tras la unificación del backend (Fases 4-5) los
conteos de cuartetas re-baselined son 147 (`ejemplo.pig`), 183 (`ejemplo.y`) y 204
(`Persona.z`)**; el `.c` de cada uno se genera con el render directo de la cuartetas (ya
no hay conteo separado del pase C3D). Notas: `BinaryOpNode` acepta `<=`/`>=`/`%` (las
gramáticas los producen); `UnaryOpNode` acepta `"non"` (Pig Latin) y `"!"` (Y?/Z).

Casos manuales que también deben seguir pasando (errores esperados): estructura con campo de tipo desconocido,
campo duplicado, función duplicada, llamada con cantidad/tipo de argumentos incorrectos,
parámetro por referencia de tipo primitivo, `retornar valor` en función sin retorno,
`romper` fuera de ciclo/`elegir`, clase con atributo duplicado, `new Clase` sin
constructor compatible, método sobrecargado con argumentos que no calzan, atributo
resuelto sin `this.` dentro de un método, `if(obj == null)`, ternario con condición no
booleana o ramas incompatibles, `new int[x]` con tamaño no entero.

---

## 15. Estado por componente (matriz)

| Componente | Estado |
|---|---|
| Corrección de arquitectura (AST/semántica unificados) | Aplicada y compilando |
| Gramática Pig Latin | Terminada |
| Gramática Y? (INDENT/DEDENT) | Terminada |
| Gramática Zetariano (real) | Terminada |
| AST unificado (34 clases: +`Return`/`Switch`/`Case`/`Conditional`/`NewArray`) | Terminado |
| `PigLatinASTBuilder` | Terminado (emite `ast.*`) |
| `YLangASTBuilder` / `ZetarianoASTBuilder` | Terminados (emiten `ast.*`; `sourceLanguage` = `Y?`/`Zetariano`; `children` en orden; `!` crudo) |
| Pase A (`registrarEstructura`/`registrarFuncion`/`registrarClase`/`registrarNativas`) + Pase B | Operativos; Pase B analiza los 34 nodos |
| `Node.analizar` | Implementado en 34/34 nodos (fases Pig Latin + Y? + Zetariano) |
| `TypeCompat` (+ reglas `NULL`) / `Symbol` (firmas, nativas, dims, `variable()`) / `Scope` (`resolveLocal`, orden) / `SymbolTable.registro` | Terminados |
| `traducir` (34 nodos) | Implementado en 34/34 (Fase 4): `Expresion.traducir` → `MemoryAccess`; `Sentencia.traducir` → `void`; `ProgramNode`/decl/import son `Sentencia`; `ParameterNode` es `Node` sin `traducir` |
| Cuartetas tipadas (`c3d/cuartetas` + `c3d/access`) | Hecho (Fase 4) — los nodos emiten `Cuarteta` tipadas; `toCCode` por cuarteta |
| `C3DGenerator.generate` / `c3d.C3DInstruction` / `ir.Cuarteta` plana | ELIMINADOS (Fase 5) — el render de cada cuarteta tipada es a la vez C3D y C |
| `CCodeGenerator.generate` | Hecho (Fase 5) — `generate(List<Cuarteta>, SymbolTable, tiposTemporales)` ensambla el `.c` (structs, arreglos, funciones, main) |
| Resolución de imports + tabla de símbolos en la UI | Operativa (`.pig`/`.y`/`.z`; la tabla incluye locales, params y nativas) |
| Reporte de errores en la UI | Conectado (sintácticos + semánticos + archivo==clase en `ventanaErrores`) |
| Reporte de cuartetas en la UI | Conectado (se llena desde `getInstrucciones()` tras el análisis semántico) |
| Reportes de C3D/código C en la UI | Conectados (Fase 5): `ventanaC3D` = render `toCCode`; "Ver código C" abre `VentanaCodigoC` con `codigoCActual` |
| Pipeline UI | Completo para compilación: `.pig`/`.y`/`.z` → sintaxis + imports + 2 pases + símbolos + cuartetas → C3D/C |

---

## 16. Pendientes priorizados y dependencias (entrada para las fases)

Orden recomendado de fases (cada una debe terminar compilando y verificada desde la GUI):

1. **P1 — Alineamiento de tipos y dims del AST: HECHO** — `fromYLang` alineado;
   modelo `tipoElemento`+`dimensiones` (`VariableDeclNode`/`ArrayDeclNode`/`ParameterNode`/
   `Symbol`); `%` y `<=`/`>=` en `BinaryOpNode`; `"!"` en `UnaryOpNode`; desugar de `+=`.
2. **P2 — Nodos faltantes: HECHO** — `ReturnNode`, `SwitchNode`/`CaseNode`,
   `ConditionalNode`, `Clase.NULO`+`Type.NULL`, `NewArrayNode`, todos con `analizar`.
3. **P3 — `YLangASTBuilder`: HECHO** (+ `children` en orden, `!` crudo, multidim).
4. **P4 — `ZetarianoASTBuilder`: HECHO** (+ ternario, `null`, `new` arreglos, desugar,
   `cuerpoOSentencia`, `aplanarRamaElse`).
5. **P5 — Semántica: HECHA en los 3 lenguajes** (specs + nativas + arreglos-como-valor):
   `analizar()` en 34/34 nodos; verificada con `Ejemplos/` (0 errores en los 3) y sondas
   de árboles/negativos. Menores restantes en §8.7.
6. **P6 — Conectar UI: HECHO** (pipeline en 3 lenguajes, errores a `ventanaErrores`,
   chequeo archivo==clase). Resta robustez: verificación sintáctica previa de imports.
7. **P7 — Cuartetas tipadas: HECHO (Fase 4)** — `traducir` en 34/34 nodos eliminando la
   emisión plana: los nodos emiten directamente cuartetas tipadas
   (`c3d.cuartetas.*` con accesos de `c3d.access`) vía `CodigoContexto.agregar`; el
   `IntermediateCodeGenerator` recibe la tabla de símbolos (`resolverSimbolo`, mapa plano
   first-wins) y mantiene `getInstrucciones()`/`getTiposTemporales()`;
   `Operandos`/`Impresion` en `ir`; sobrecarga por conteo de argumentos, decoración
   `Clase_método_idx`/`Clase_init_idx`, marcadores `func`/`func_end`/`array`/`halt` (Pig
   Latin) + reporte `ventanaCuartetas`.
   Verificado: `mvn -B clean compile` + traducción de
   `Ejemplos/{ejemplo.pig,ejemplo.y,Persona.z}` (0 errores semánticos, cuartetas tipadas
   revisadas a mano). *Dependió de P5.*
8. **P8+P9 — C3D = C (una sola fuente): HECHO (Fase 5)** — se ELIMINÓ el pase intermedio
   (`C3DGenerator`, `C3DInstruction`, `ir.Cuarteta` plana): cada cuarteta tipada
   implementa `toCCode` que es a la vez C3D y C final. `CCodeGenerator` toma
   `generate(getInstrucciones(), tabla, generador.getTiposTemporales())` y ensambla el
   `.c`; `VentanaPrincipal` alimenta `ventanaCuartetas`/`ventanaC3D`/`codigoCActual`
   desde `getInstrucciones()`. Re-baseline de conteos: 147/183/204. *Dependió de P7.*

Otros pendientes registrados: Pase A recursivo a imports‑de‑imports. Decisión de C destino
de structs/clases tomada en P9: structs → `struct` de C; clases → `struct` + funciones
libres (`Clase_metodo_i(...)` con `self` explícito); instancias siempre punteros
(`malloc` + `->`), con limitaciones documentadas en §11.5 (retorno de arreglos → `void*`,
`==` de cadenas compara punteros, structs declarados por valor se bajan a puntero sin
inicializar).

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
   por idioma son `grammar`, `astbuilder` y los token makers (`ui`).
5. **Un nodo, una implementación**: cualquier nodo nuevo va al paquete `ast` (o
   `ast/decl`) y los 3 builders lo comparten.
6. **No romper la API existente** sin justificarlo en el plan: `Node`, constructores de
   nodos, `SymbolTable`, `ContextoSemantico` y `SemanticAnalyzer` son la interfaz pública.
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
| Cuarteta | Instrucción de código intermedio tipada (`c3d.cuartetas.*`); su `toCCode` es a la vez C3D y código C |
| Pase A | Pasada de la semántica que registra firmas/declaraciones sin revisar cuerpos |
| Pase B | Pasada que verifica los cuerpos aprovechando el catálogo de Pase A |
| INDENT / DEDENT | Tokens sintéticos que delimitan bloques indentados en Y? |
| TokenMaker | Clase de RSyntaxTextArea para el resaltado de sintaxis |
| Visitor | Patrón de diseño para recorrer el AST (eliminado: no se usaba, ver 6.3) |
| Vocabulary | Traducción de nombre de tipo → `Type` (hoy en `Type.fromPigLatin`/`fromYLang`/`fromZetariano`) |