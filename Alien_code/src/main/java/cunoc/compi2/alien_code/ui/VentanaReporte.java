package cunoc.compi2.alien_code.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class VentanaReporte extends JDialog {

    private final DefaultTableModel modelo;

    public VentanaReporte(Window propietario, String titulo, String[] columnas) {
        super(propietario, titulo);
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tabla.getTableHeader().setReorderingAllowed(false);

        setContentPane(new JScrollPane(tabla));
        setPreferredSize(new Dimension(800, 400));
        setMinimumSize(new Dimension(400, 250));
        pack();
        setLocationRelativeTo(propietario);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setDatos(List<Object[]> filas) {
        modelo.setRowCount(0);
        for (Object[] fila : filas) {
            modelo.addRow(fila);
        }
    }

    public void limpiar() {
        modelo.setRowCount(0);
    }
}
