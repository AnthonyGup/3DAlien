package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import cunoc.compi2.alien_code.runner.RarsRunner;
import cunoc.compi2.alien_code.runner.RuntimeRiscV;

public class VentanaCodigoRISCV extends JDialog {

    private static final Color BG_DARK = new Color(0x1E, 0x1E, 0x1E);
    private static final Color FG_LIGHT = new Color(0xD4, 0xD4, 0xD4);
    private static final Color SELECTION_BG = new Color(0x3A, 0x3D, 0x41);

    private final RSyntaxTextArea area;
    private final PanelLog log;
    private final File carpetaProyecto;
    private final String contenidoOriginal;

    public VentanaCodigoRISCV(Window propietario, String titulo, String contenido, PanelLog log, File carpetaProyecto) {
        super(propietario, titulo);
        this.log = log;
        this.carpetaProyecto = carpetaProyecto;
        this.contenidoOriginal = contenido;

        Font font = new Font("JetBrains Mono", Font.PLAIN, 13);
        if (!font.getFamily().equals("JetBrains Mono")) {
            font = new Font("Consolas", Font.PLAIN, 13);
        }
        if (!font.getFamily().equals("Consolas")) {
            font = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }

        area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle("text/x-asm");
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setAutoIndentEnabled(true);
        area.setTabSize(4);
        area.setEditable(false);
        area.setText(contenido);
        area.setCaretPosition(0);
        area.setBackground(BG_DARK);
        area.setForeground(FG_LIGHT);
        area.setCaretColor(FG_LIGHT);
        area.setSelectionColor(SELECTION_BG);
        area.setMargin(new Insets(8, 12, 8, 12));
        ((javax.swing.text.DefaultCaret) area.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);

        JButton botonGuardar = new JButton("Guardar .s");
        estilizarBoton(botonGuardar, new Color(0x0E, 0x63, 0x9C));
        botonGuardar.addActionListener(e -> guardar());

        JButton botonEjecutarRARS = new JButton("Ejecutar en RARS");
        estilizarBoton(botonEjecutarRARS, new Color(0x0E, 0x63, 0x9C));
        botonEjecutarRARS.addActionListener(e -> ejecutarEnRARS());

        JButton botonCopiar = new JButton("Copiar");
        estilizarBoton(botonCopiar, new Color(0x3A, 0x3D, 0x41));
        botonCopiar.addActionListener(e -> { area.selectAll(); area.copy(); area.select(0, 0); });

        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelInferior.setBackground(BG_DARK);
        panelInferior.add(botonCopiar);
        panelInferior.add(botonGuardar);
        panelInferior.add(botonEjecutarRARS);

        RTextScrollPane scrollPane = new RTextScrollPane(area);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBackground(BG_DARK);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        getContentPane().setBackground(BG_DARK);

        setPreferredSize(new Dimension(950, 600));
        setMinimumSize(new Dimension(500, 300));
        pack();
        setLocationRelativeTo(propietario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void estilizarBoton(JButton b, Color bg) {
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(bg.brighter());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(bg);
            }
        });
    }

    private void guardar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar código RISC-V");
        if (carpetaProyecto != null && carpetaProyecto.isDirectory()) {
            chooser.setCurrentDirectory(carpetaProyecto);
        }
        chooser.setSelectedFile(new File("programa.s"));
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

    private void ejecutarEnRARS() {
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
        if (!baseDir.exists()) baseDir.mkdirs();

        File sFile = new File(baseDir, "programa.s");
        try {
            String ejecutable = area.getText() + "\n" + RuntimeRiscV.texto();
            Files.writeString(sFile.toPath(), ejecutable, StandardCharsets.UTF_8);
            log.agregarInfo("> Código RISC-V (programa + runtime) guardado en: "
                    + sFile.getAbsolutePath());
        } catch (IOException ex) {
            log.agregarError("No se pudo escribir .s: " + ex.getMessage());
            return;
        }

        log.agregarInfo("> Ejecutando en RARS (embebido): " + sFile.getAbsolutePath() + " ...");
        String asm;
        try {
            asm = Files.readString(sFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.agregarError("No se pudo leer .s: " + ex.getMessage());
            return;
        }

        new Thread(() -> {
            try {
                RarsRunner.Resultado r = RarsRunner.ejecutar(asm, "");
                SwingUtilities.invokeLater(() -> {
                    if (r.estado == RarsRunner.Estado.OK) {
                        log.agregarExito("Ejecución RARS exitosa (exit=" + r.codigoSalida + ")");
                    } else {
                        log.agregarError("RARS " + r.estado
                                + (r.mensaje.isEmpty() ? "" : ": " + r.mensaje));
                    }
                    if (!r.salida.isEmpty()) {
                        log.agregarInfo(r.salida);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        log.agregarError("Excepción ejecutando RARS: " + ex.getMessage()));
            }
        }).start();
    }
}