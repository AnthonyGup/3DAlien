package cunoc.compi2.alien_code.ui;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class VentanaMenuBar extends JMenuBar {

    private final JButton botonCompilarMain;
    private final JButton botonLimpiarLog;
    private final JButton botonGuardarCambios;
    private final JMenu menuArchivo;
    private final JMenu menuReportes;
    private final JMenu menuAyuda;

    public VentanaMenuBar() {
        menuArchivo = new JMenu("Archivo");
        menuArchivo.add(item("Nuevo archivo"));
        menuArchivo.add(item("Abrir carpeta de proyecto"));
        menuArchivo.addSeparator();
        menuArchivo.add(item("Guardar"));
        menuArchivo.addSeparator();
        menuArchivo.add(item("Salir"));
        add(menuArchivo);

        botonGuardarCambios = crearBoton("Guardar cambios");
        botonGuardarCambios.setToolTipText("Guardar cambios del archivo abierto");
        add(botonGuardarCambios);

        menuReportes = new JMenu("Reportes");
        menuReportes.add(item("Ver errores"));
        menuReportes.add(item("Ver tabla de s\u00edmbolos"));
        menuReportes.add(item("Ver cuartetas"));
        menuReportes.add(item("Ver c\u00f3digo C generado"));
        add(menuReportes);

        menuAyuda = new JMenu("Ayuda");
        menuAyuda.add(item("Acerca de"));
        add(menuAyuda);

        botonCompilarMain = crearBoton("Compilar main (.pig)");
        add(botonCompilarMain);

        botonLimpiarLog = crearBoton("Limpiar log");
        add(botonLimpiarLog);
    }

    private JMenuItem item(String texto) {
        return new JMenuItem(texto);
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boton.setOpaque(true);
                boton.setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boton.setOpaque(false);
            }
        });
        return boton;
    }

    public void setOnNuevoArchivo(ActionListener l) { anclar(0, l); }
    public void setOnAbrirCarpeta(ActionListener l) { anclar(1, l); }
    public void setOnGuardar(ActionListener l) { anclar(3, l); }
    public void setOnGuardarCambios(ActionListener l) { botonGuardarCambios.addActionListener(l); }
    public void setOnSalir(ActionListener l) { anclar(5, l); }
    public void setOnVerErrores(ActionListener l) { anclarReportes(0, l); }
    public void setOnVerSimbolos(ActionListener l) { anclarReportes(1, l); }
    public void setOnVerCuartetas(ActionListener l) { anclarReportes(2, l); }
    public void setOnVerCodigoC(ActionListener l) { anclarReportes(3, l); }
    public void setOnAcercaDe(ActionListener l) { anclarAyuda(0, l); }
    public void setOnCompilarMain(ActionListener l) { botonCompilarMain.addActionListener(l); }
    public void setOnLimpiarLog(ActionListener l) { botonLimpiarLog.addActionListener(l); }

    private void anclar(int indice, ActionListener l) {
        ((JMenuItem) menuArchivo.getMenuComponent(indice)).addActionListener(l);
    }

    private void anclarReportes(int indice, ActionListener l) {
        ((JMenuItem) menuReportes.getMenuComponent(indice)).addActionListener(l);
    }

    private void anclarAyuda(int indice, ActionListener l) {
        ((JMenuItem) menuAyuda.getMenuComponent(indice)).addActionListener(l);
    }
}
