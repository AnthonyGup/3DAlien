package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class VentanaCodigoC extends JDialog {

    private final RSyntaxTextArea area;
    private final PanelLog log;
    private final File carpetaProyecto;

    public VentanaCodigoC(Window propietario, String titulo, String contenido, PanelLog log, File carpetaProyecto) {
        super(propietario, titulo);
        this.log = log;
        this.carpetaProyecto = carpetaProyecto;

        area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle("text/x-c");
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setAutoIndentEnabled(true);
        area.setTabSize(4);
        area.setEditable(false);
        area.setText(contenido);
        area.setCaretPosition(0);

        JButton botonGuardar = new JButton("Guardar .c");
        botonGuardar.addActionListener(e -> guardar());

        JButton botonCompilarC = new JButton("Compilar C");
        botonCompilarC.addActionListener(e -> compilarC());

        JButton botonCopiar = new JButton("Copiar");
        botonCopiar.addActionListener(e -> { area.selectAll(); area.copy(); area.select(0, 0); });

        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelInferior.add(botonCopiar);
        panelInferior.add(botonGuardar);
        panelInferior.add(botonCompilarC);

        RTextScrollPane scrollPane = new RTextScrollPane(area);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(950, 600));
        setMinimumSize(new Dimension(500, 300));
        pack();
        setLocationRelativeTo(propietario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void guardar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar código C");
        if (carpetaProyecto != null && carpetaProyecto.isDirectory()) {
            chooser.setCurrentDirectory(carpetaProyecto);
        }
        chooser.setSelectedFile(new File("programa.c"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destino = chooser.getSelectedFile();
            try {
                Files.writeString(destino.toPath(), area.getText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Guardado: " + destino.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void compilarC() {
        if (log == null) {
            JOptionPane.showMessageDialog(this, "No hay log disponible para mostrar salida.");
            return;
        }

        File baseDir;
        if (carpetaProyecto != null && carpetaProyecto.isDirectory()) {
            baseDir = new File(carpetaProyecto, "build");
        } else {
            baseDir = new File(System.getProperty("user.dir"), "build");
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        File cFile = new File(baseDir, "programa.c");
        try {
            Files.writeString(cFile.toPath(), area.getText(), StandardCharsets.UTF_8);
            log.agregarInfo("> Código C guardado en: " + cFile.getAbsolutePath());
        } catch (IOException ex) {
            log.agregarError("No se pudo escribir .c: " + ex.getMessage());
            return;
        }

        String compilador = detectarCompilador();
        if (compilador == null) {
            log.agregarError("No se encontró compilador C (gcc, clang o cl.exe en PATH).");
            return;
        }

        File exeFile = new File(baseDir, esWindows() ? "programa.exe" : "programa");
        List<String> cmd = new ArrayList<>();
        cmd.add(compilador);
        if (compilador.contains("cl.exe")) {
            cmd.add("/Fe" + exeFile.getAbsolutePath());
            cmd.add(cFile.getAbsolutePath());
        } else {
            cmd.add("-o");
            cmd.add(exeFile.getAbsolutePath());
            cmd.add(cFile.getAbsolutePath());
        }

        log.agregarInfo("> Compilando C con " + compilador + " en " + baseDir.getAbsolutePath() + "...");
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(baseDir);
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                StringBuilder salida = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        salida.append(linea).append('\n');
                    }
                }
                int exit = proc.waitFor();

                String out = salida.toString().trim();
                int finalExit = exit;
                String finalOut = out;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (finalExit == 0) {
                        log.agregarExito("Compilación C exitosa. Ejecutable: " + exeFile.getAbsolutePath());
                        if (!finalOut.isEmpty()) log.agregarInfo(finalOut);
                    } else {
                        log.agregarError("Error compilando C (código " + finalExit + "):");
                        if (!finalOut.isEmpty()) log.agregarError(finalOut);
                    }
                });
            } catch (IOException | InterruptedException ex) {
                javax.swing.SwingUtilities.invokeLater(() -> log.agregarError("Excepción compilando C: " + ex.getMessage()));
            }
        }).start();
    }

    private String detectarCompilador() {
        String[] candidatos = {"gcc", "clang", "cl.exe"};
        for (String c : candidatos) {
            if (existeEnPath(c)) return c;
        }
        return null;
    }

    private boolean existeEnPath(String comando) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        String sep = File.pathSeparator;
        for (String dir : path.split(sep)) {
            File f = new File(dir, comando);
            if (f.isFile() && f.canExecute()) return true;
            if (esWindows() && new File(dir, comando + ".exe").isFile()) return true;
        }
        return false;
    }

    private boolean esWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}