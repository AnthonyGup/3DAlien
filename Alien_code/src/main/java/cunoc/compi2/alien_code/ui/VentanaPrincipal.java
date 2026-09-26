package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.decl.ClassDeclNode;
import cunoc.compi2.alien_code.ast.program.ImportNode;
import cunoc.compi2.alien_code.ast.program.ProgramNode;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.codegen.CCodeGenerator;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.pigLatin.astbuilder.PigLatinASTBuilder;
import cunoc.compi2.alien_code.ylang.astbuilder.YLangASTBuilder;
import cunoc.compi2.alien_code.zetariano.astbuilder.ZetarianoASTBuilder;
import cunoc.compi2.alien_code.semantic.SemanticAnalyzer;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

public class VentanaPrincipal extends JFrame {

    private static final String[] COLUMNAS_ERRORES = {"Tipo", "Descripción", "Línea", "Columna"};
    private static final String[] COLUMNAS_SIMBOLOS = {"Nombre", "Tipo", "Clase", "Tamaño", "Valor", "Línea"};
    private static final String[] COLUMNAS_CUARTETAS = {"#", "Operador", "Operando1", "Operando2", "Resultado"};
    private static final String[] COLUMNAS_C3D = {"#", "Instrucción"};

    private final MainPanel mainPanel;
    private final VentanaMenuBar menuBar;
    private final VentanaReporte ventanaErrores;
    private final VentanaReporte ventanaSimbolos;
    private final VentanaReporte ventanaCuartetas;
    private final VentanaReporte ventanaC3D;
    private File carpetaProyecto;
    private String codigoCActual;

    public VentanaPrincipal() {
        super("3DAlien - Compilador de Y?, Zetariano y Pig Latin");
        this.setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        PanelArbolProyecto arbolProyecto = new PanelArbolProyecto();
        PanelEditorTabs editorTabs = new PanelEditorTabs();
        PanelLog panelLog = new PanelLog();
        mainPanel = new MainPanel(arbolProyecto, editorTabs, panelLog);

        menuBar = new VentanaMenuBar();
        setJMenuBar(menuBar);

        setContentPane(mainPanel);

        ventanaErrores = new VentanaReporte(this, "Reporte de errores", COLUMNAS_ERRORES);
        ventanaSimbolos = new VentanaReporte(this, "Tabla de símbolos", COLUMNAS_SIMBOLOS);
        ventanaCuartetas = new VentanaReporte(this, "Cuartetas", COLUMNAS_CUARTETAS);
        ventanaC3D = new VentanaReporte(this, "Código C3D", COLUMNAS_C3D);

        arbolProyecto.setListenerArchivo(archivo -> mainPanel.getEditorTabs().abrirArchivo(archivo));

        conectarAcciones(arbolProyecto, editorTabs, panelLog);
    }

    private void conectarAcciones(PanelArbolProyecto arbol, PanelEditorTabs tabs, PanelLog log) {
        menuBar.setOnNuevoArchivo(e -> crearNuevoArchivo(arbol));
        menuBar.setOnAbrirCarpeta(e -> abrirCarpetaProyecto());
        menuBar.setOnGuardar(e -> guardar(tabs));
        menuBar.setOnSalir(e -> dispose());
        menuBar.setOnCompilarMain(e -> compilarMainPig(log));
        menuBar.setOnLimpiarLog(e -> log.limpiar());
        menuBar.setOnVerErrores(e -> ventanaErrores.setVisible(true));
        menuBar.setOnVerSimbolos(e -> ventanaSimbolos.setVisible(true));
        menuBar.setOnVerCuartetas(e -> ventanaCuartetas.setVisible(true));
        menuBar.setOnVerC3D(e -> ventanaC3D.setVisible(true));
        menuBar.setOnVerCodigoC(e -> verCodigoC(log));
        menuBar.setOnAcercaDe(e -> acercaDe());
    }

    private void crearNuevoArchivo(PanelArbolProyecto arbol) {
        if (carpetaProyecto == null) {
            JOptionPane.showMessageDialog(this, "Abre una carpeta de proyecto primero.");
            return;
        }
        File archivo = PanelArbolProyecto.mostrarDialogoNuevoArchivo(this, carpetaProyecto);
        if (archivo == null) return;
        try {
            if (archivo.createNewFile()) {
                mainPanel.getEditorTabs().abrirArchivo(archivo);
                arbol.recargar();
            }
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirCarpetaProyecto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Abrir carpeta de proyecto");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.carpetaProyecto = chooser.getSelectedFile();
            this.setTitle("3DAlien - " + carpetaProyecto.getName());
            mainPanel.getArbolProyecto().setRaizProyecto(carpetaProyecto);
            mainPanel.getPanelLog().limpiar();
            mainPanel.getPanelLog().agregar("Proyecto abierto: " + carpetaProyecto.getAbsolutePath());
        }
    }

    private void guardar(PanelEditorTabs tabs) {
        EditorPanel editor = tabs.getEditorSeleccionado();
        if (editor == null) return;
        if (editor.getArchivo() == null) {
            JOptionPane.showMessageDialog(this, "No hay archivo asociado a esta pestaña.");
            return;
        }
        guardarEn(editor.getArchivo(), editor.getText());
    }

    private void guardarEn(File archivo, String contenido) {
        try {
            java.nio.file.Files.writeString(archivo.toPath(), contenido);
            mainPanel.getPanelLog().agregar("Guardado: " + archivo.getName());
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compilarMainPig(PanelLog log) {
        log.limpiar();
        ventanaErrores.limpiar();
        ventanaSimbolos.limpiar();

        if (carpetaProyecto == null) {
            log.agregarError("No hay proyecto abierto. Abre una carpeta de proyecto primero.");
            return;
        }

        File mainPig = buscarMainPig(carpetaProyecto);
        if (mainPig == null) {
            log.agregarError("No se encontró ningún archivo .pig con sección principal (MAIOR>).");
            return;
        }

        log.agregarInfo("> Compilando main: " + mainPig.getName());
        VerificadorSintactico verificador = new VerificadorSintactico();
        VerificadorSintactico.Resultado resultado = verificador.verificar(leer(mainPig), mainPig);

        if (!resultado.extensionValida) {
            log.agregarError("Extensión no reconocida.");
            return;
        }

        log.agregarInfo("> Análisis léxico: " + resultado.cantidadTokens + " tokens encontrados.");
        log.agregarInfo("> Análisis sintáctico...");

        if (resultado.hayErrores()) {
            List<Object[]> filas = new ArrayList<>();
            for (VerificadorSintactico.ErrorSintactico error : resultado.errores) {
                filas.add(new Object[]{"Sintáctico", error.mensaje, error.linea, error.columna});
                log.agregarError("[Sintáctico] "
                    + (error.linea > 0 ? "Línea " + error.linea + ", Columna " + error.columna + ": " : "")
                    + error.mensaje);
            }
            ventanaErrores.setDatos(filas);
            log.agregarError("Se detectaron " + resultado.errores.size()
                + " error(es) sintácticos. Ver Reportes > Ver errores.");
            return;
        }

        log.agregarExito("Sin errores sintácticos.");
        ejecutarPipelineSemantico(mainPig, log, ventanaErrores);
    }

    private File buscarMainPig(File directorio) {
        File[] archivos = directorio.listFiles();
        if (archivos == null) return null;

        for (File archivo : archivos) {
            if (archivo.isDirectory()) {
                File encontrado = buscarMainPig(archivo);
                if (encontrado != null) return encontrado;
            } else if (archivo.getName().toLowerCase().endsWith(".pig")) {
                String contenido = leer(archivo);
                if (contenido != null && contenido.contains("MAIOR>")) {
                    return archivo;
                }
            }
        }
        return null;
    }

    private void ejecutarPipelineSemantico(File archivo, PanelLog log, VentanaReporte ventanaErrores) {
        ErrorListener errores = new ErrorListener();
        List<ProgramNode> programas = new ArrayList<>();
        List<Object[]> filasErrores = new ArrayList<>();

        ProgramNode programa = construirAST(archivo);
        if (programa == null) {
            log.agregarError("No se pudo construir el AST de " + archivo.getName() + ".");
            return;
        }
        programas.add(programa);

        if (extensionDe(archivo).equals("z")) {
            String esperado = archivo.getName().replaceFirst("(?i)\\.z$", "");
            String clase = nombreClasePrincipal(programa);
            if (clase != null && !clase.equals(esperado)) {
                filasErrores.add(new Object[]{"Semántico",
                    "El archivo debe llamarse igual que la clase pública '" + clase + "'.",
                    programa.getLine(), programa.getColumn()});
            }
        }

        for (ImportNode importacion : importsDe(programa)) {
            File archivoImportado = resolverImport(importacion.rutaCompleta);
            if (archivoImportado == null) {
                filasErrores.add(new Object[]{"Semántico",
                    "No se encontró el archivo importado '" + importacion.rutaCompleta + "'.",
                    importacion.getLine(), importacion.getColumn()});
                continue;
            }
            ProgramNode programaImportado = construirAST(archivoImportado);
            if (programaImportado == null) {
                log.agregarPendiente("El constructor de AST de " + extensionDe(archivoImportado).toUpperCase()
                        + " aún no está implementado (import '" + importacion.rutaCompleta + "').");
                continue;
            }
            programas.add(programaImportado);
        }

        SemanticAnalyzer analizador = new SemanticAnalyzer(errores);
        analizador.analizar(programas);

        if (errores.hasErrors()) {
            for (cunoc.compi2.alien_code.errors.CompilerError error : errores.getErrors()) {
                filasErrores.add(new Object[]{"Semántico", error.getMessage(),
                    error.getLine(), error.getColumn()});
            }
        }

        if (!filasErrores.isEmpty()) {
            for (Object[] fila : filasErrores) {
                String tipo = String.valueOf(fila[0]);
                String mensaje = String.valueOf(fila[1]);
                int linea = ((Number) fila[2]).intValue();
                int columna = ((Number) fila[3]).intValue();
                log.agregarError("[" + tipo + "] "
                    + (linea > 0 ? "Línea " + linea + ", Columna " + columna + ": " : "")
                    + mensaje);
            }
            ventanaErrores.setDatos(filasErrores);
            log.agregarError("Se detectaron " + filasErrores.size()
                + " error(es). Ver Reportes > Ver errores.");
            return;
        }

        log.agregarExito("Análisis semántico completado.");
        mostrarSimbolos(analizador.getSymbolTable());

        ventanaCuartetas.limpiar();
        IntermediateCodeGenerator generador = new IntermediateCodeGenerator(errores,
                analizador.getSymbolTable());
        for (ProgramNode p : programas) {
            p.traducir(generador);
        }
        List<Cuarteta> instrucciones = generador.getInstrucciones();
        List<Object[]> filasCuartetas = new ArrayList<>();
        int contador = 0;
        for (Cuarteta cuarteta : instrucciones) {
            filasCuartetas.add(new Object[]{++contador, cuarteta.operator(),
                textoDe(cuarteta.getOperand1()), textoDe(cuarteta.getOperand2()),
                textoDe(cuarteta.getResult())});
        }
        ventanaCuartetas.setDatos(filasCuartetas);
        log.agregarExito("Cuartetas generadas: " + contador
            + ". Ver Reportes > Ver cuartetas.");

        ventanaC3D.limpiar();
        List<Object[]> filasC3D = new ArrayList<>();
        int numeroC3D = 0;
        StringBuilder linea = new StringBuilder();
        for (Cuarteta instruccion : instrucciones) {
            linea.setLength(0);
            instruccion.toCCode(linea);
            filasC3D.add(new Object[]{++numeroC3D, linea.toString()});
        }
        ventanaC3D.setDatos(filasC3D);
        codigoCActual = new CCodeGenerator().generate(instrucciones,
                analizador.getSymbolTable(), generador.getTiposTemporales());
        log.agregarExito("C3D generado (" + numeroC3D + " instrucciones). Ver Reportes > Ver C3D.");
        log.agregarExito("Código C generado. Ver Reportes > Ver código C.");
    }

    private String textoDe(MemoryAccess acceso) {
        if (acceso == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        acceso.toCCode(sb);
        return sb.toString();
    }

    private ProgramNode construirAST(File archivo) {
        String codigo = leer(archivo);
        if (codigo == null) return null;
        switch (extensionDe(archivo)) {
            case "pig":
                return new PigLatinASTBuilder().construir(codigo);
            case "y":
                return new YLangASTBuilder().construir(codigo);
            case "z":
                return new ZetarianoASTBuilder().construir(codigo);
            default:
                return null;
        }
    }

    private String nombreClasePrincipal(ProgramNode programa) {
        for (Node declaracion : programa.declarations) {
            if (declaracion instanceof ClassDeclNode clase) {
                return clase.nombre;
            }
        }
        return null;
    }

    private List<ImportNode> importsDe(ProgramNode programa) {
        List<ImportNode> imports = new ArrayList<>();
        for (Node nodo : programa.declarations) {
            if (nodo instanceof ImportNode) {
                imports.add((ImportNode) nodo);
            }
        }
        return imports;
    }

    private File resolverImport(String rutaCompleta) {
        if (carpetaProyecto == null) return null;
        String ruta = rutaCompleta.replace('.', File.separatorChar);
        File base = new File(carpetaProyecto, ruta);
        File archivoY = new File(base.getPath() + ".y");
        if (archivoY.isFile()) return archivoY;
        File archivoZ = new File(base.getPath() + ".z");
        if (archivoZ.isFile()) return archivoZ;
        return null;
    }

    private String leer(File archivo) {
        try {
            return Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String extensionDe(File archivo) {
        if (archivo == null) return "";
        String nombre = archivo.getName().toLowerCase();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1);
    }

    private void mostrarSimbolos(SymbolTable symbolTable) {
        List<Object[]> filas = new ArrayList<>();
        for (Symbol simbolo : symbolTable.listarSimbolos()) {
            filas.add(new Object[]{
                    simbolo.getName(),
                    simbolo.getType() != null ? String.valueOf(simbolo.getType()) : "vacio",
                    simbolo.getKind().name(),
                    simbolo.getSize(),
                    "-",
                    simbolo.getLinea() > 0 ? simbolo.getLinea() : "-"
            });
        }
        ventanaSimbolos.setDatos(filas);
    }

    private void verCodigoC(PanelLog log) {
        if (codigoCActual == null) {
            log.agregarPendiente("Compila un archivo primero para generar código C.");
            return;
        }
        VentanaCodigoC ventana = new VentanaCodigoC(this, "Código C generado", codigoCActual, log, carpetaProyecto);
        ventana.setVisible(true);
    }

    private void acercaDe() {
        JOptionPane.showMessageDialog(this,
                "3DAlien - Compilador de Y?, Zetariano y Pig Latin\n"
                        + "Proyecto de Compiladores 2, CUNOC.\n"
                        + "Genera código C a partir de tres lenguajes alienígenas.",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }
}
