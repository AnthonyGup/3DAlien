package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

public class EditorPanel extends JPanel {

    private final RSyntaxTextArea textArea;
    private final JLabel statusLabel;
    private File archivo;

    public EditorPanel() {
        this(null);
    }

    public EditorPanel(File archivo) {
        setLayout(new BorderLayout());
        this.archivo = archivo;

        TokenMakerFactory.setDefaultInstance(new ProyectoTokenMakerFactory());

        textArea = new RSyntaxTextArea(25, 60);
        textArea.setSyntaxEditingStyle(estiloSegunExtension(archivo));
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setTabSize(4);

        if (archivo != null && archivo.isFile()) {
            try {
                textArea.setText(java.nio.file.Files.readString(archivo.toPath()));
            } catch (java.io.IOException ignorada) {
            }
        }

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Lin: 1, Col: 1");
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                int linea = textArea.getCaretLineNumber() + 1;
                int columna = textArea.getCaretOffsetFromLineStart() + 1;
                statusLabel.setText("Lin: " + linea + ", Col: " + columna);
            }
        });
    }

    public static String estiloSegunExtension(File archivo) {
        if (archivo == null) {
            return ProyectoTokenMakerFactory.LENGUAJE_YLANG;
        }
        String nombre = archivo.getName().toLowerCase();
        if (nombre.endsWith(".z")) {
            return ProyectoTokenMakerFactory.LENGUAJE_ZETARIANO;
        }
        if (nombre.endsWith(".pig")) {
            return ProyectoTokenMakerFactory.LENGUAJE_PIGLATIN;
        }
        return ProyectoTokenMakerFactory.LENGUAJE_YLANG;
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public File getArchivo() {
        return archivo;
    }

    public void setArchivo(File archivo) {
        this.archivo = archivo;
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String texto) {
        textArea.setText(texto);
    }
}
