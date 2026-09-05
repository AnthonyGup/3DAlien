package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class PanelLog extends JPanel {

    private final JTextArea area;

    public PanelLog() {
        setLayout(new BorderLayout());

        area = new JTextArea(10, 60);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(40, 40, 40));
        area.setForeground(new Color(220, 220, 220));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void agregar(String linea) {
        area.append(linea + "\n");
        area.setCaretPosition(area.getDocument().getLength());
    }

    public void limpiar() {
        area.setText("");
    }
}
