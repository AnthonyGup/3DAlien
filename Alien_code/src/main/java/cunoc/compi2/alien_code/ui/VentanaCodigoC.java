package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class VentanaCodigoC extends JDialog {

    private final JTextArea area;
    private final PanelLog log;

    public VentanaCodigoC(Window propietario, String titulo, String contenido, PanelLog log) {
        super(propietario, titulo);
        this.log = log;
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setTabSize(4);
        area.setText(contenido);
        area.setCaretPosition(0);

        JButton botonGuardar = new JButton("Guardar...");
        botonGuardar.addActionListener(e -> guardar());

        JButton botonCompilarC = new JButton("Compilar C");
        botonCompilarC.addActionListener(e -> compilarC());

        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelInferior.add(botonGuardar);
        panelInferior.add(botonCompilarC);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(850, 500));
        setMinimumSize(new Dimension(400, 250));
        pack();
        setLocationRelativeTo(propietario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void guardar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar código C");
        chooser.setSelectedFile(new File("codigo.c"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destino = chooser.getSelectedFile();
            try {
                Files.writeString(destino.toPath(), area.getText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Guardado: " + destino.getAbsolutePath());
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void compilarC() {
        if (log == null) {
            JOptionPane.showMessageDialog(this, "No hay log disponible para mostrar salida.");
            return;
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "3dalien_build_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) {
            log.agregarError("No se pudo crear directorio temporal.");
            return;
        }
        File cFile = new File(tempDir, "programa.c");
        try {
            Files.writeString(cFile.toPath(), area.getText(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.agregarError("No se pudo escribir .c temporal: " + ex.getMessage());
            return;
        }

        String compilador = detectarCompilador();
        if (compilador == null) {
            log.agregarError("No se encontró compilador C (gcc, clang o cl.exe en PATH).");
            return;
        }

        File exeFile = new File(tempDir, esWindows() ? "programa.exe" : "programa");
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

        log.agregarInfo("> Compilando C con " + compilador + "...");
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(tempDir);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            StringBuilder salida = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    salida.append(linea).append('\n');
                }
            }
            int exit = proc.waitFor();
            if (exit == 0) {
                log.agregarExito("Compilación C exitosa. Ejecutable: " + exeFile.getAbsolutePath());
                if (salida.length() > 0) log.agregarInfo(salida.toString().trim());
            } else {
                log.agregarError("Error compilando C (código " + exit + "):");
                if (salida.length() > 0) log.agregarError(salida.toString().trim());
            }
        } catch (IOException | InterruptedException ex) {
            log.agregarError("Excepción compilando C: " + ex.getMessage());
        }
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