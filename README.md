# 3DAlien

3DAlien es una aplicación de escritorio desarrollada para el curso de Compiladores 2 del CUNOC. Compila programas escritos en Pig Latin, Y? y Zetariano a partir de un árbol de sintaxis abstracta común. El resultado incluye cuartetas, código C y ensamblador RISC-V compatible con RARS.

La interfaz permite editar archivos, compilar un programa principal, consultar reportes, guardar el código generado y ejecutar el ensamblador mediante el simulador RARS integrado.

## Lenguajes

| Extensión | Lenguaje | Uso en el proyecto |
|---|---|---|
| `.pig` | Pig Latin | Programa principal y punto de entrada `MAIOR>` / `FINIS;`. |
| `.y` | Y? | Funciones y estructuras que pueden importarse desde el programa principal. |
| `.z` | Zetariano | Clases, atributos, métodos, constructores y herencia. |

El archivo `.pig` activo declara sus dependencias mediante `import`. Por ejemplo, `import control.Control.z` corresponde a `control/Control.z`, a partir de la carpeta que contiene el archivo principal.

## Requisitos

- Java 21 o superior para ejecutar el JAR.
- JDK 21 o superior y Maven 3.9 o superior para compilar desde el código fuente.
- Un entorno gráfico para la interfaz Swing.
- Opcionalmente, GCC, Clang o `cl.exe` en el `PATH` para compilar el código C generado.

El JAR incluye ANTLR, RSyntaxTextArea y RARS. No incluye la máquina virtual de Java.

## Ejecutar la aplicación

Desde esta carpeta, abre con doble clic `Alien_code/target/Alien_code-1.0-SNAPSHOT.jar` si Windows tiene los archivos `.jar` asociados a Java. También puede ejecutarse desde una terminal:

```powershell
java -jar Alien_code/target/Alien_code-1.0-SNAPSHOT.jar
```

Si el JAR todavía no existe o se modificó el código fuente, genera una versión actualizada:

```powershell
cd Alien_code
mvn clean package
java -jar target/Alien_code-1.0-SNAPSHOT.jar
```

La compilación necesita el archivo local `Alien_code/libs/rars.jar` y acceso a los repositorios de Maven cuando las dependencias todavía no se encuentran en la caché local.

## Uso básico

1. Selecciona **Archivo > Abrir carpeta de proyecto** y abre la carpeta del programa.
2. Abre el archivo `.pig` principal desde el árbol y guarda los cambios del editor.
3. Con el `.pig` seleccionado, presiona **Compilar main (.pig)**.
4. Consulta el log y el menú **Reportes** para revisar errores, símbolos, cuartetas y código generado.
5. En **Ver código RISC-V generado**, utiliza **Guardar .s** para exportar el ensamblador o **Ejecutar en RARS** para ejecutarlo con el simulador integrado.
6. En **Ver código C generado**, utiliza **Guardar .c** o **Compilar C** si hay un compilador C disponible.

La opción de ejecución RARS de la interfaz entrega una entrada vacía al programa; no proporciona una consola interactiva para leer datos durante la simulación.

## Estructura

```text
3DAlien/
├── README.md
├── MANUAL_TECNICO.md
└── Alien_code/
    ├── pom.xml
    ├── libs/rars.jar
    ├── src/main/antlr4/       Gramáticas ANTLR
    ├── src/main/java/         Compilador, interfaz y generadores de código
    └── target/                Archivos generados por Maven
```

## Documentación

El [manual técnico](Alien_code/docs/MANUAL_TECNICO.md) describe la arquitectura,
el proceso de compilación, la generación de código y las limitaciones conocidas.
