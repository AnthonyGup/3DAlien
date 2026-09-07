package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
        menuBar.setOnDescargarProyecto(e -> descargarProyecto());
        menuBar.setOnSalir(e -> dispose());
        menuBar.setOnCompilar(e -> compilar(log));
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

    private void descargarProyecto() {
        if (carpetaProyecto == null) {
            JOptionPane.showMessageDialog(this, "No hay proyecto abierto.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar proyecto comprimido (zip)");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destino = new File(chooser.getSelectedFile(), carpetaProyecto.getName() + ".zip");
            try {
                comprimirZip(carpetaProyecto, destino);
                mainPanel.getPanelLog().agregar("Proyecto descargado: " + destino.getAbsolutePath());
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo comprimir el proyecto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void comprimirZip(File origen, File destino) throws java.io.IOException {
        try (java.util.zip.ZipOutputStream zos =
                new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(destino))) {
            comprimirDirectorio(origen, origen.getName(), zos);
        }
    }

    private void comprimirDirectorio(File dir, String base, java.util.zip.ZipOutputStream zos)
            throws java.io.IOException {
        File[] archivos = dir.listFiles();
        if (archivos == null) return;
        byte[] buffer = new byte[4096];
        for (File archivo : archivos) {
            String rutaRelativa = base + "/" + archivo.getName();
            if (archivo.isDirectory()) {
                zos.putNextEntry(new java.util.zip.ZipEntry(rutaRelativa + "/"));
                zos.closeEntry();
                comprimirDirectorio(archivo, rutaRelativa, zos);
            } else {
                zos.putNextEntry(new java.util.zip.ZipEntry(rutaRelativa));
                try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo)) {
                    int leidos;
                    while ((leidos = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, leidos);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private void compilar(PanelLog log) {
        log.limpiar();
        ventanaErrores.limpiar();

        EditorPanel editor = mainPanel.getEditorTabs().getEditorSeleccionado();
        if (editor == null) {
            log.agregarError("No hay ninguna pesta\u00f1a abierta.");
            return;
        }

        File archivo = editor.getArchivo();
        VerificadorSintactico verificador = new VerificadorSintactico();
        VerificadorSintactico.Resultado resultado = verificador.verificar(editor.getText(), archivo);

        String nombreArchivo = archivo == null ? "(sin archivo)" : archivo.getName();
        log.agregarInfo("> Compilando " + nombreArchivo + (resultado.extensionValida ? " (" + resultado.lenguaje + ")" : ""));

        if (!resultado.extensionValida) {
            log.agregarError("Extensi\u00f3n no reconocida. Usa .y, .z o .pig.");
            return;
        }

        log.agregarInfo("> An\u00e1lisis l\u00e9xico: " + resultado.cantidadTokens + " tokens encontrados.");
        log.agregarInfo("> An\u00e1lisis sint\u00e1ctico...");

        if (resultado.hayErrores()) {
            List<Object[]> filas = new ArrayList<>();
            for (VerificadorSintactico.ErrorSintactico error : resultado.errores) {
                log.agregarError("[Error sint\u00e1ctico] l\u00ednea " + error.linea + ", columna " + error.columna + ": " + error.mensaje);
                filas.add(new Object[]{"Sint\u00e1ctico", error.mensaje, error.linea, error.columna});
            }
            ventanaErrores.setDatos(filas);
            return;
        }

        log.agregarExito("Sin errores sint\u00e1cticos.");
        log.agregarPendiente("> An\u00e1lisis sem\u00e1ntico, cuartetas, C3D y C: pendiente (backend en desarrollo).");
    }

    private void verCodigoC(PanelLog log) {
        log.agregar("El código C generado se mostrará aquí cuando el pipeline esté conectado.");
    }

    private void acercaDe() {
        JOptionPane.showMessageDialog(this,
                "3DAlien - Compilador de Y?, Zetariano y Pig Latin\n"
                        + "Proyecto de Compiladores 2, CUNOC.\n"
                        + "Genera código C a partir de tres lenguajes alienígenas.",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }
}
