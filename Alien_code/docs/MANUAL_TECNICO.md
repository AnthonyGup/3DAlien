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

El directorio `3DAlien` contiene la aplicación y esta documentación; `pruebas` se encuentra en su directorio padre.

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
        │   ├── runner/              Integración con RARS y runtime
        │   └── Alien_code.java      Punto de entrada
        └── resources/riscv/runtime.s
pruebas/
├── fase1/
└── fase2/
```

El directorio `Alien_code/target` se crea durante la compilación. Contiene clases, fuentes generadas por ANTLR y el JAR; no es el lugar para editar el código fuente del proyecto.

## 4. Arquitectura general

El flujo principal es el siguiente:

```text
Archivo .pig activo e importaciones directas .y/.z
                       |
                       v
          Analizadores ANTLR por lenguaje
                       |
                       v
        Constructores de AST por lenguaje
                       |
                       v
                AST compartido
                       |
                       v
   Análisis semántico y tabla de símbolos
                       |
                       v
       Código intermedio: lista de cuartetas
                       |
             +---------+---------+
             |                   |
             v                   v
      Generador de C      Generador de RISC-V
             |                   |
             v                   v
      Compilador C       Runtime + simulador RARS
```

`VentanaPrincipal` coordina la compilación iniciada desde la interfaz. Los constructores de cada lenguaje transforman sus árboles de análisis a nodos de `ast`. El análisis semántico, el código intermedio y los generadores trabajan sobre estas estructuras comunes.

## 5. Lenguajes y archivos fuente

| Lenguaje | Extensión | Función principal |
|---|---|---|
| Pig Latin | `.pig` | Define el programa principal y puede importar archivos de los otros lenguajes. |
| Y? | `.y` | Permite definir funciones y estructuras reutilizables. |
| Zetariano | `.z` | Permite definir clases y operaciones orientadas a objetos. |

Un ejemplo de programa principal existente es [`Control.pig`](../pruebas/fase2/04_control/Control.pig):

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

## 7. Análisis semántico

`SemanticAnalyzer` recibe una lista de nodos `ProgramNode` que contiene el programa principal y sus importaciones cargadas. El análisis se organiza en dos recorridos:

1. **Pase A:** registra declaraciones, estructuras, funciones y clases. Para las clases registra sus nombres, resuelve superclases, detecta ciclos de herencia, ordena padres antes que hijos y revisa miembros y constructores.
2. **Registro de funciones nativas:** incorpora funciones según los lenguajes presentes, como `imprimir`, `leer`, `print` y `println`.
3. **Pase B:** analiza declaraciones y cuerpos con la información registrada, verifica usos de símbolos y aplica las reglas semánticas correspondientes.

`SymbolTable`, `Scope` y `Symbol` representan ámbitos, identificadores y sus atributos. `ContextoSemanticoImpl` ofrece operaciones de consulta, definición y registro de errores para los nodos. `TypeCompat` y las clases de apoyo de programación orientada a objetos participan en la comprobación de tipos, herencia y miembros.

Si hay errores sintácticos o semánticos, la interfaz los registra y detiene el flujo antes de generar las cuartetas de esa compilación. El reporte de símbolos presenta nombre, tipo, clase de símbolo, tamaño y línea disponible.

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

### 10.3 Runtime y ejecución

`src/main/resources/riscv/runtime.s` contiene rutinas auxiliares utilizadas por el código generado. `RuntimeRiscV` lo carga como recurso del classpath. Al presionar **Ejecutar en RARS**, `PanelCodigoRISCV` concatena el código del panel con el runtime y guarda el resultado en `build/programa.s` de la carpeta del proyecto. El botón **Guardar .s**, en cambio, guarda el texto mostrado en el panel; para ejecutar ese archivo por separado puede ser necesario agregarle el runtime.

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

El proyecto utiliza `maven-antrun-plugin` durante `process-classes` para incorporar las clases y recursos del `libs/rars.jar` local. `maven-shade-plugin` empaqueta las dependencias Maven y establece `cunoc.compi2.alien_code.Alien_code` como clase principal del manifiesto. El runtime se incorpora como recurso de la aplicación.

Para iniciar el resultado:

```powershell
java -jar target/Alien_code-1.0-SNAPSHOT.jar
```

También puede abrirse mediante doble clic cuando el sistema tenga configurada la asociación de archivos `.jar` con una versión compatible de Java. Maven puede advertir que RARS utiliza una dependencia `system` con una ruta dentro del proyecto; el archivo `libs/rars.jar` debe mantenerse disponible al construir.

## 13. Pruebas y validación

Las carpetas de `pruebas/fase2` son programas independientes. Para una revisión manual, se abre la carpeta correspondiente, se selecciona su `.pig`, se compila y se consultan los reportes y el código generado.

| Carpeta | Aspectos principales |
|---|---|
| `01_tictactoe` | Tablero, arreglos bidimensionales, herencia, sobrescritura y polimorfismo. |
| `02_vida` | Juego de la Vida, arreglos bidimensionales y ciclos. |
| `03_banco` | Clases y funciones importadas en un escenario bancario. |
| `03_estructuras` | Pila, cola, referencias, herencia y estructuras enlazadas. |
| `04_control` | Condicionales, `switch`, ciclos, operadores y control de flujo. |
| `05_errores` | Casos negativos de semántica; las expectativas están en `ERRORES_ESPERADOS.txt`. |
| `06_laberinto` | Interacción de clases, tablero y lógica de un laberinto. |
| `7_Arena` | Caso integrado con clases, funciones y control de flujo. |

Algunas carpetas incluyen `SALIDA_ESPERADA.txt`; no todas lo incluyen. Estos archivos son referencias de los casos, pero una ejecución debe contrastarse con el comportamiento real y con los datos de entrada disponibles. Para revisar una modificación del backend RISC-V, se recomienda guardar el ensamblador anterior, generar uno nuevo con los mismos fuentes y comparar ambos, además de ejecutar casos representativos.

En las verificaciones realizadas después de la unificación de cargas, los ensambladores de TicTacToe, GameOfLife, Banco, Estructuras, Control, Laberinto y Arena fueron idénticos a sus respectivas versiones anteriores. Arena alcanzó el límite de pasos durante esa ejecución. En TicTacToe, la segunda partida terminó en siete turnos, mientras que el archivo de salida esperada describe nueve; el ensamblador coincidía con el anterior al cambio. Los resultados de ejecución de GameOfLife, Estructuras y Control coincidieron con sus referencias al ignorar diferencias de saltos de línea.

El ciclo `mvn clean package` compila el proyecto, pero no sustituye estas pruebas funcionales; en la configuración observada no hay pruebas Java automatizadas que se ejecuten durante ese ciclo.

## 14. Diagnóstico y mantenimiento

| Situación | Revisión recomendada |
|---|---|
| El JAR no abre con doble clic | Comprobar `java -version`, la asociación de `.jar` y ejecutar `java -jar` desde una terminal para ver el error. |
| Maven no encuentra RARS | Verificar que exista `Alien_code/libs/rars.jar` y ejecutar Maven desde `Alien_code`. |
| No se encuentra una importación | Revisar que el archivo exista y que la ruta con puntos corresponda a la ubicación relativa al `.pig`. |
| La compilación usa una versión anterior del archivo | Guardar la pestaña activa antes de compilar, porque el AST se construye leyendo el archivo desde disco. |
| No aparece un compilador C | Instalar GCC, Clang o `cl.exe` y agregarlos al `PATH` de la aplicación. |
| RARS reporta un error de ensamblado | Revisar el log, el código generado y la presencia del runtime en el archivo ejecutado. |
| RARS alcanza el límite de pasos | Revisar los ciclos del programa y sus entradas; el límite protege la interfaz de ejecuciones prolongadas. |
| Un programa necesita entrada durante la simulación | La acción gráfica envía entrada vacía; debe ejecutarse mediante un mecanismo que suministre la entrada antes de iniciar RARS. |

Para extender un lenguaje, el orden recomendado es modificar su gramática, adaptar su constructor de AST, comprobar las reglas semánticas y verificar la traducción de los nodos a cuartetas. Si la nueva instrucción afecta el código objetivo, deben actualizarse los generadores correspondientes y probarse ambos destinos. Los archivos generados en `target` no deben editarse manualmente.
