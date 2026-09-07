package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PanelLog extends JPanel {

    private final JTextPane area;
    private final StyledDocument documento;

    public PanelLog() {
        setLayout(new BorderLayout());

        area = new JTextPane();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(40, 40, 40));
        area.setForeground(new Color(220, 220, 220));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        documento = area.getStyledDocument();
        crearEstilos();

        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    private void crearEstilos() {
        agregarEstilo("info", new Color(220, 220, 220), false);
        agregarEstilo("exito", new Color(0, 180, 80), false);
        agregarEstilo("error", new Color(255, 80, 80), true);
        agregarEstilo("pendiente", new Color(150, 150, 150), false);
    }

    private void agregarEstilo(String nombre, Color color, boolean negrita) {
        Style estilo = area.addStyle(nombre, null);
        StyleConstants.setForeground(estilo, color);
        if (negrita) {
            StyleConstants.setBold(estilo, true);
        }
    }

    public void agregar(String linea) {
        agregarConEstilo(linea, "info");
    }

    public void agregarInfo(String linea) {
        agregarConEstilo(linea, "info");
    }

    public void agregarExito(String linea) {
        agregarConEstilo(linea, "exito");
    }

    public void agregarError(String linea) {
        agregarConEstilo(linea, "error");
    }

    public void agregarPendiente(String linea) {
        agregarConEstilo(linea, "pendiente");
    }

    private void agregarConEstilo(String linea, String nombreEstilo) {
        try {
            documento.insertString(documento.getLength(), linea + "\n", area.getStyle(nombreEstilo));
        } catch (javax.swing.text.BadLocationException ignorada) {
        }
        area.setCaretPosition(documento.getLength());
    }

    public void limpiar() {
        try {
            documento.remove(0, documento.getLength());
        } catch (javax.swing.text.BadLocationException ignorada) {
        }
    }
}