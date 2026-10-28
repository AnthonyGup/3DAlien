# Manual técnico de 3DAlien

## 1. Introducción

3DAlien es un compilador con interfaz gráfica para programas compuestos por archivos de los lenguajes Pig Latin, Y? y Zetariano. El proyecto se desarrolló para el curso de Compiladores 2 del CUNOC. Su objetivo es analizar un programa principal y sus importaciones, construir una representación común, comprobar su validez semántica y producir código intermedio y código objetivo.

La aplicación genera código C y ensamblador RISC-V. El código RISC-V puede ejecutarse mediante RARS integrado; el código C puede guardarse o compilarse con una herramienta externa instalada en el sistema.

Este documento describe la organización y el funcionamiento del código existente, además del procedimiento para compilar, ejecutar y mantener el proyecto.

## 2. Tecnologías y requisitos

| Componente | Versión o función |
|---|---|
| Java | JDK 21 o superior para compilar; Java 21 o superior para ejecutar el JAR. |
| Maven | Gestión del proyecto, dependencias y empaquetado; se recomienda Maven 3.9 o superior. |
| ANTLR | Versión 4.13.1; generación de analizadores léxicos y sintácticos. |
| Swing | Componentes de la interfaz gráfica. |
| RSyntaxTextArea | Versión 3.3.4; edición de código y resaltado de sintaxis. |
| RARS | Versión 1.6, incluida localmente en `Alien_code/libs/rars.jar`. |
| Compilador C | GCC, Clang o `cl.exe`, opcional para crear un ejecutable a partir del código C. |

Se requiere un entorno gráfico para utilizar la interfaz. Los archivos de código fuente se leen como UTF-8. La primera compilación con Maven puede necesitar acceso a Internet para descargar dependencias y complementos.

## 3. Organización del proyecto

El directorio `3DAlien` contiene la aplicación y esta documentación.

```text
3DAlien/
├── README.md
├── MANUAL_TECNICO.md
└── Alien_code/
    ├── pom.xml
    ├── libs/rars.jar
    └── src/main/
        ├── antlr4/cunoc/compi2/alien_code/
        │   ├── pigLatin/grammar/
        │   ├── ylang/grammar/
        │   └── zetariano/grammar/
        ├── java/cunoc/compi2/alien_code/
        │   ├── ast/                 Nodos del AST compartido
        │   ├── c3d/                 Cuartetas y accesos a memoria
        │   ├── codegen/             Generadores de C y RISC-V
        │   ├── errors/              Diagnósticos
        │   ├── ir/                  Generación de código intermedio
        │   ├── pigLatin/astbuilder/ Constructor del AST de Pig Latin
        │   ├── semantic/            Símbolos y análisis semántico
        │   ├── ui/                  Interfaz gráfica
        │   ├── ylang/astbuilder/    Constructor del AST de Y?
        │   ├── zetariano/astbuilder/ Constructor del AST de Zetariano
        │   ├── runner/              Integración con RARS
        │   └── Alien_code.java      Punto de entrada
```

El directorio `Alien_code/target` se crea durante la compilación. Contiene clases, fuentes generadas por ANTLR y el JAR; no es el lugar para editar el código fuente del proyecto.

## 4. Arquitectura general y diagrama de clases

El flujo principal es el siguiente:

```text
┌──────────────────────┐
│ VentanaPrincipal     │
│ Interfaz Swing       │
└──────────┬───────────┘
           │ compila archivo .pig e imports
           v
┌──────────────────────┐     ┌──────────────────────┐
│ PigLatinASTBuilder   │     │ YLangASTBuilder      │
└──────────┬───────────┘     └──────────┬───────────┘
           │                            │
           │                 ┌──────────v───────────┐
           │                 │ ZetarianoASTBuilder  │
           │                 └──────────┬───────────┘
           └──────────────┬─────────────┘
                          v
                 ┌──────────────────┐
                 │ AST compartido   │
                 │ ast.*            │
                 └────────┬─────────┘
                          v
                 ┌──────────────────┐
                 │ SemanticAnalyzer │
                 │ SymbolTable      │
                 │ TypeCompat       │
                 └────────┬─────────┘
                          v
                 ┌──────────────────┐
                 │ IntermediateCode │
                 │ Generator         │
                 └────────┬─────────┘
                          v
                 ┌──────────────────┐
                 │ Cuartetas / IR   │
                 └──────┬─────┬─────┘
                        │     │
             ┌──────────v┐   ┌v────────────────┐
             │ CCode     │   │ RiscVCode      │
             │ Generator │   │ Generator      │
             └──────┬────┘   └──────┬─────────┘
                    v               v
             Código C         Ensamblador RISC-V
                    │               │
                    v               v
             GCC/Clang/cl.exe   RarsRunner / RARS
```

`VentanaPrincipal` coordina la compilación iniciada desde la interfaz. Los constructores de cada lenguaje transforman sus árboles de análisis a nodos de `ast`. El análisis semántico, el código intermedio y los generadores trabajan sobre estas estructuras comunes.

El diagrama de clases lógico puede resumirse así:

```text
ProgramNode
├── ImportNode
├── ClassDeclNode / StructDeclNode
├── FunctionDeclNode / MethodDeclNode
├── VariableDeclNode / ArrayDeclNode
├── IfNode / WhileNode / ForNode / SwitchNode
├── AssignmentNode / ReturnNode
└── BinaryOpNode / UnaryOpNode / AccessNode

SemanticAnalyzer
├── SymbolTable
├── Scope
├── Symbol
└── TypeCompat

IntermediateCodeGenerator
└── Cuarteta
    ├── Asignacion3D
    ├── Operacion3D
    ├── Condicional3D
    ├── Goto3D / Etiqueta3D
    ├── Llamada3D / LlamadaVirtual3D
    ├── Imprimir3D / Leer3D
    └── Retornar3D
```

## 5. Lenguajes y archivos fuente

| Lenguaje | Extensión | Función principal |
|---|---|---|
| Pig Latin | `.pig` | Define el programa principal y puede importar archivos de los otros lenguajes. |
| Y? | `.y` | Permite definir funciones y estructuras reutilizables. |
| Zetariano | `.z` | Permite definir clases y operaciones orientadas a objetos. |

Un programa principal debe tener extensión `.pig` y contener las secciones
`MAIOR>` y `FINIS;`. Sus imports se resuelven desde la carpeta del programa:

```text
import control.Control.z

VARIABILES>
esto c : novus Control() : Control ;

MAIOR>
>> "Suma: " >> c.operar(1, 7, 3) ;
FINIS;
```

La ruta de una importación se escribe con puntos. En el ejemplo, `control.Control.z` se resuelve como `control/Control.z` dentro de la carpeta que contiene el `.pig`. La interfaz carga las importaciones declaradas directamente en el programa principal; no debe asumirse que recorre importaciones transitivas.

Para compilar desde la interfaz, el archivo activo debe tener extensión `.pig` y contener `MAIOR>`. Es importante guardar las modificaciones: la comprobación sintáctica inicial utiliza el texto del editor, mientras que la construcción posterior del AST vuelve a leer el archivo desde disco.

## 6. Análisis léxico y sintáctico

Las gramáticas están en `Alien_code/src/main/antlr4/cunoc/compi2/alien_code/`:

- Pig Latin: `pigLatin/grammar/PigLatin.g4`.
- Y?: `ylang/grammar/YLangLexer.g4` y `YLangParser.g4`.
- Zetariano: `zetariano/grammar/Zetariano.g4`.

El complemento de ANTLR genera los analizadores en `target/generated-sources/antlr4` durante el ciclo de compilación. Maven agrega esas fuentes generadas al compilador Java.

Cada lenguaje tiene un constructor específico: `PigLatinASTBuilder`, `YLangASTBuilder` o `ZetarianoASTBuilder`. Su función es recorrer la estructura producida por ANTLR y crear nodos del modelo común. Este modelo incluye programas, declaraciones, expresiones y sentencias. Los diagnósticos sintácticos se recopilan mediante las clases del paquete `errors` y se muestran en el log y en el reporte de errores.

Al modificar una gramática debe editarse el archivo `.g4`, no la fuente Java generada. Después se ejecuta `mvn clean package` para regenerar y compilar los analizadores.

### 6.1 Palabras reservadas

Las palabras reservadas no pueden utilizarse como identificadores en el lenguaje correspondiente.

| Lenguaje | Palabras reservadas |
|---|---|
| Pig Latin | `import`, `esto`, `series`, `novus`, `null`, `finis`, `si`, `aliter`, `dum`, `facere`, `per`, `perge`, `interrumpe`, `non`, `verum`, `falsus`, `numerus`, `decimalis`, `textum`, `littera`, `bool`, `variabiles>`, `maior>` |
| Y? | `estructura`, `definir`, `retornar`, `si`, `entonces`, `sino`, `contrario`, `elegir`, `caso`, `siempre`, `romper`, `continuar`, `para`, `mientras`, `hacer`, `entero`, `flotante`, `cadena`, `caracter`, `bool`, `verdadero`, `falso`, `%estructuras`, `%funciones` |
| Zetariano | `class`, `public`, `private`, `protected`, `extends`, `this`, `super`, `@Override`, `void`, `new`, `null`, `true`, `false`, `if`, `else`, `switch`, `case`, `default`, `break`, `continue`, `for`, `while`, `do`, `return`, `int`, `double`, `char`, `boolean`, `String` |

### 6.2 Símbolos y operadores

Los tres lenguajes comparten los operadores principales, aunque cada uno tiene símbolos de sección y sentencias propios.

| Categoría | Pig Latin | Y? | Zetariano |
|---|---|---|---|
| Aritmética | `+ - * /` | `+ - * /` | `+ - * / %` |
| Comparación | `== != < > <= >=` | `== != < > <= >=` | `== != < > <= >=` |
| Lógica | `&& ||`, `non` | `&& ||`, `!` | `&& ||`, `!` |
| Asignación | `=` | `=` | `=`, `+=`, `-=`, `*=` |
| Incremento | `++`, `--` | `++`, `--` | `++`, `--` |
| Delimitadores | `() {} [] , . : ;` | `() {} [] , . : ;` | `() {} [] , . : ;` |
| Texto y caracteres | `"texto"`, `'c'` | `"texto"`, `'c'` | `"texto"`, `'c'` |
| Secciones | `VARIABILES>`, `MAIOR>`, `FINIS` | `%estructuras`, `%funciones` | clase delimitada por `{}` |
| Entrada y salida | `<<`, `>>` | funciones registradas por el compilador | llamadas y sentencias del lenguaje |

Los comentarios son `//` y `/* ... */` en Y? y Zetariano. Pig Latin utiliza `//` y `## ... ##`.

### 6.3 Estructura gramatical resumida

```text
programaPig
    ::= importacion* variables? principal EOF
principal
    ::= "MAIOR>" sentencia* "FINIS" ";"

programaY
    ::= saltos* estructuras? funciones EOF
funcionY
    ::= "definir" nombre "(" parametros? ")" ("->" tipo)? ":" INDENT sentencia+ DEDENT

programaZ
    ::= visibilidad? "class" nombre ("extends" nombre)?
        "{" miembroClase* "}" EOF
miembroClase
    ::= campo | constructor | metodo
```

En Y? los tokens `INDENT` y `DEDENT` son fabricados por el lexer a partir de la indentación. En Pig Latin y Zetariano los bloques se delimitan con llaves o con las marcas propias del lenguaje. Las expresiones siguen una precedencia de menor a mayor: `||`, `&&`, igualdad, relacionales, suma/resta, multiplicación/división y operadores unarios.

## 7. Análisis semántico

`SemanticAnalyzer` recibe una lista de nodos `ProgramNode` que contiene el programa principal y sus importaciones cargadas. El análisis se organiza en dos recorridos:

1. **Pase A:** registra declaraciones, estructuras, funciones y clases. Para las clases registra sus nombres, resuelve superclases, detecta ciclos de herencia, ordena padres antes que hijos y revisa miembros y constructores.
2. **Registro de funciones nativas:** incorpora funciones según los lenguajes presentes, como `imprimir`, `leer`, `print` y `println`.
3. **Pase B:** analiza declaraciones y cuerpos con la información registrada, verifica usos de símbolos y aplica las reglas semánticas correspondientes.

`SymbolTable`, `Scope` y `Symbol` representan ámbitos, identificadores y sus atributos. `ContextoSemanticoImpl` ofrece operaciones de consulta, definición y registro de errores para los nodos. `TypeCompat` y las clases de apoyo de programación orientada a objetos participan en la comprobación de tipos, herencia y miembros.

Si hay errores sintácticos o semánticos, la interfaz los registra y detiene el flujo antes de generar las cuartetas de esa compilación. El reporte de símbolos presenta nombre, tipo, clase de símbolo, tamaño y línea disponible.

### 7.1 Tipos del modelo común

El AST utiliza `INT`, `FLOAT`, `STRING`, `BOOL`, `CHAR`, `VOID`, `STRUCT`, `CLASS`, `ARRAY` y `NULL`. Los nombres equivalentes en cada lenguaje son:

| Tipo común | Pig Latin | Y? | Zetariano |
|---|---|---|---|
| `INT` | `numerus` | `entero` | `int` |
| `FLOAT` | `decimalis` | `flotante` | `double` |
| `STRING` | `textum` | `cadena` | `String` |
| `CHAR` | `littera` | `caracter` | `char` |
| `BOOL` | `bool` | `bool` | `boolean` |
| `VOID` | no disponible como tipo primitivo | no disponible explícitamente | `void` |
| referencia | estructura/clase | estructura o identificador | clase o arreglo |
| nulo | `null` | no tiene literal propio en la gramática | `null` |

### 7.2 Tabla de compatibilidad y conversiones

La tabla siguiente describe `TypeCompat.tipoAritmetico` y las conversiones que utiliza `BinaryOpNode` para generar cuartetas. `—` significa que la operación es rechazada semánticamente.

| Operación | INT | FLOAT | STRING | CHAR | BOOL |
|---|---:|---:|---:|---:|---:|
| `INT +` | `INT` | `FLOAT` | `STRING` | — | `STRING` |
| `FLOAT +` | `FLOAT` | `FLOAT` | `STRING` | — | `STRING` |
| `STRING +` | `STRING` | `STRING` | `STRING` | `STRING` | `STRING` |
| `CHAR +` | — | — | `STRING` | — | — |
| `BOOL +` | — | — | `STRING` | — | — |

Para `-`, `*`, `/` y `%`, sólo se permiten operandos numéricos: `INT` con `INT` produce `INT`; cualquier combinación válida con `FLOAT` produce `FLOAT`, excepto `%`, que se calcula como residuo entero después de convertir los operandos. Una cadena no se convierte implícitamente a número.

Cuando una suma involucra una cadena, el otro operando se convierte así:

| Tipo original | Conversión interna | Resultado |
|---|---|---|
| `INT` | `strd` | `STRING` |
| `FLOAT` | `strn` | `STRING` |
| `CHAR` | `strc` | `STRING` |
| `BOOL` | `strd` | `STRING` |
| `STRING` | sin conversión | `STRING` |

Las comparaciones `==` y `!=` aceptan tipos iguales, combinaciones numéricas, y `NULL` con strings, estructuras o clases. La igualdad de strings compara su contenido, no sólo sus direcciones. Las comparaciones `<`, `>`, `<=` y `>=` aceptan números y caracteres. `&&` y `||` requieren booleanos.

La asignación permite convertir `INT` a `FLOAT`. También permite asignar `NULL` a `STRING`, `STRUCT` y `CLASS`. No se convierten automáticamente strings a números, números a strings fuera de una concatenación, ni valores booleanos a números.

## 8. Código intermedio

`IntermediateCodeGenerator` traduce los nodos de los programas a una lista de objetos `Cuarteta`. Esta representación permite que los dos generadores consuman las mismas instrucciones.

Entre las cuartetas se encuentran asignaciones, operaciones, etiquetas, saltos, condiciones, llamadas, retornos, impresión, lectura y declaraciones de arreglos. Los operandos se representan mediante `MemoryAccess` y sus implementaciones, por ejemplo `Literal3D`, `NameAccess`, `TemporalAccess`, `CampoAccess` e `IndiceAccess`.

El generador también conserva información de temporales y layouts de clases, utilizada posteriormente por los generadores de C y RISC-V. La interfaz muestra cada cuarteta con operador, operandos y resultado mediante **Reportes > Ver cuartetas**.

## 9. Generación de código C

`CCodeGenerator` recibe las cuartetas, la tabla de símbolos, los tipos de temporales y la información de layouts. Con estos datos construye el código C que se presenta en **Reportes > Ver código C generado**.

La ventana permite copiar o guardar el código. Al seleccionar **Compilar C**, la aplicación:

1. Guarda el texto como `build/programa.c` dentro de la carpeta del proyecto abierto.
2. Busca `gcc`, `clang` o `cl.exe` en el `PATH`.
3. Invoca el compilador en un hilo separado y muestra su salida en el log.
4. Si la compilación es correcta, genera `build/programa.exe` en Windows o `build/programa` en otros sistemas.

La generación de código C no requiere un compilador C instalado; la acción **Compilar C** sí lo requiere. Esta acción construye el ejecutable, pero no lo ejecuta automáticamente.

## 10. Generación de RISC-V

### 10.1 Componentes

`RiscVCodeGenerator` crea un `ContextoRiscV`, indexa las instrucciones y delega la generación del cuerpo a `GeneradorCuerpo` y `RiscVEmisor`. El paquete `codegen/riscv/emisores` separa responsabilidades como memoria, aritmética, control, llamadas y entrada/salida.

Las clases principales son:

| Clase | Responsabilidad |
|---|---|
| `ContextoRiscV` | Mantiene información de generación, registros de datos, temporales, etiquetas y errores. |
| `LayoutsRiscV` | Consulta offsets, tipos de campos y datos de clases y estructuras. |
| `MarcoRiscV` | Representa los parámetros y el tamaño del marco de una función. |
| `ConvencionLlamada` | Distribuye argumentos entre registros enteros, registros de punto flotante y pila. |
| `AccesoMemoria` | Emite cargas y almacenamientos con offsets directos o grandes. |
| `EmisorMemoria` | Emite cargas, almacenamientos, cálculo de direcciones y reservas de heap. |
| `RiscVCodeGenerator` | Construye las secciones de datos y texto del resultado. |

### 10.2 Datos, registros y llamadas

El generador identifica su salida como RISC-V `RV32IMFD` para RARS 1.6. En `.data` reserva espacio para variables globales, el heap y un área para cadenas, además de emitir constantes y cadenas registradas durante la traducción. Las cadenas se codifican en UTF-8 y se terminan con un byte cero.

El heap reservado por el generador es de 3 145 728 bytes y el área `Str_libre` es de 131 072 bytes. Los slots utilizados por la representación de memoria ocupan ocho bytes; el marco de función se alinea a 16 bytes. Los parámetros enteros y de referencia utilizan hasta ocho registros de argumentos enteros, y los parámetros `double` hasta ocho registros de argumentos de punto flotante; los argumentos que exceden esas cantidades se colocan en la pila.

`EmisorMemoria` distingue cargas enteras y de punto flotante, y calcula direcciones para campos e índices mediante un conjunto de registros temporales. Los valores `double` se cargan con `fld`; las cargas de palabras usan `lw`. La traducción realiza conversiones cuando la operación concreta las requiere.

### 10.3 Ejecución

Las operaciones de entrada, salida, conversión, comparación de strings, concatenación y manejo de errores se emiten directamente desde los emisores RISC-V. No existe un runtime RISC-V separado que deba copiarse o concatenarse al ensamblador generado. `PanelCodigoRISCV` guarda y ejecuta directamente el texto producido por `RiscVCodeGenerator`.

`RarsRunner` ensambla y ejecuta el texto mediante la API de RARS. Normaliza los saltos CRLF y elimina un BOM inicial si existe. La ejecución utiliza un límite de 30 000 000 de instrucciones por defecto y devuelve uno de los estados `OK`, `ERROR_ENSAMBLADO`, `ERROR_EJECUCION` o `LIMITE_PASOS`. La interfaz ejecuta la simulación en un hilo separado y muestra la salida y el estado en el log.

La API de `RarsRunner` acepta una cadena de entrada completa antes de simular. La acción de la interfaz actualmente envía una cadena vacía, por lo que no es una entrada interactiva y los programas que requieren datos pueden no completar el recorrido esperado desde esa opción.

## 11. Interfaz gráfica

El punto de entrada `Alien_code.main` crea `VentanaPrincipal` mediante `SwingUtilities.invokeLater`. La ventana reúne el árbol de archivos, las pestañas del editor, el log y el panel de código RISC-V.

| Control | Función |
|---|---|
| Archivo > Abrir carpeta de proyecto | Selecciona la carpeta cuyos archivos se muestran en el árbol. |
| Archivo > Nuevo archivo | Crea un archivo en el proyecto abierto. |
| Archivo > Guardar | Guarda el contenido de la pestaña activa. |
| Compilar main (.pig) | Compila el `.pig` activo, no otro archivo encontrado automáticamente. |
| Reportes > Ver errores | Muestra tipo, descripción, línea y columna de los errores reportados. |
| Reportes > Ver tabla de símbolos | Muestra los símbolos registrados. |
| Reportes > Ver cuartetas | Muestra las instrucciones intermedias. |
| Reportes > Ver código C generado | Abre el visor de C con opciones de copia, guardado y compilación. |
| Reportes > Ver código RISC-V generado | Muestra el ensamblador y las opciones de copia, guardado y ejecución. |

Al compilar, la carpeta usada para resolver importaciones se sincroniza con la carpeta padre del `.pig` activo. Si una compilación falla, se debe revisar el reporte y corregir los archivos antes de volver a compilar.

## 12. Compilación y empaquetado

Desde `3DAlien/Alien_code`:

```powershell
mvn clean package
```

El ciclo genera los analizadores de ANTLR, compila las clases Java y construye:

```text
target/Alien_code-1.0-SNAPSHOT.jar
```

El proyecto utiliza `maven-antrun-plugin` durante `process-classes` para incorporar las clases y recursos del `libs/rars.jar` local. `maven-shade-plugin` empaqueta las dependencias Maven y establece `cunoc.compi2.alien_code.Alien_code` como clase principal del manifiesto. Las operaciones RISC-V forman parte de los emisores del compilador.

Para iniciar el resultado:

```powershell
java -jar target/Alien_code-1.0-SNAPSHOT.jar
```

También puede abrirse mediante doble clic cuando el sistema tenga configurada la asociación de archivos `.jar` con una versión compatible de Java. Maven puede advertir que RARS utiliza una dependencia `system` con una ruta dentro del proyecto; el archivo `libs/rars.jar` debe mantenerse disponible al construir.

## 13. Diagnóstico y mantenimiento

| Situación | Revisión recomendada |
|---|---|
| El JAR no abre con doble clic | Comprobar `java -version`, la asociación de `.jar` y ejecutar `java -jar` desde una terminal para ver el error. |
| Maven no encuentra RARS | Verificar que exista `Alien_code/libs/rars.jar` y ejecutar Maven desde `Alien_code`. |
| No se encuentra una importación | Revisar que el archivo exista y que la ruta con puntos corresponda a la ubicación relativa al `.pig`. |
| La compilación usa una versión anterior del archivo | Guardar la pestaña activa antes de compilar, porque el AST se construye leyendo el archivo desde disco. |
| No aparece un compilador C | Instalar GCC, Clang o `cl.exe` y agregarlos al `PATH` de la aplicación. |
| RARS reporta un error de ensamblado | Revisar el log y el código RISC-V generado, especialmente etiquetas, registros y offsets. |
| RARS alcanza el límite de pasos | Revisar los ciclos del programa y sus entradas; el límite protege la interfaz de ejecuciones prolongadas. |
| Un programa necesita entrada durante la simulación | La acción gráfica envía entrada vacía; debe ejecutarse mediante un mecanismo que suministre la entrada antes de iniciar RARS. |

Para extender un lenguaje, el orden recomendado es modificar su gramática, adaptar su constructor de AST, comprobar las reglas semánticas y verificar la traducción de los nodos a cuartetas. Si la nueva instrucción afecta el código objetivo, deben actualizarse los generadores correspondientes y probarse ambos destinos. Los archivos generados en `target` no deben editarse manualmente.
