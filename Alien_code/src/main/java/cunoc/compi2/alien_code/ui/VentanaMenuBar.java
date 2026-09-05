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

    private final JButton botonCompilar;
    private final JButton botonLimpiarLog;

    public VentanaMenuBar() {
        JMenu menuArchivo = new JMenu("Archivo");
        menuArchivo.add(item("Nuevo archivo"));
        menuArchivo.add(item("Abrir carpeta de proyecto"));
        menuArchivo.addSeparator();
        menuArchivo.add(item("Guardar"));
        menuArchivo.add(item("Guardar como"));
        menuArchivo.addSeparator();
        menuArchivo.add(item("Descargar proyecto (zip)"));
        menuArchivo.addSeparator();
        menuArchivo.add(item("Salir"));
        add(menuArchivo);

        JMenu menuReportes = new JMenu("Reportes");
        menuReportes.add(item("Ver errores"));
        menuReportes.add(item("Ver tabla de símbolos"));
        menuReportes.add(item("Ver cuartetas"));
        menuReportes.add(item("Ver C3D"));
        menuReportes.add(item("Ver código C generado"));
        add(menuReportes);

        JMenu menuAyuda = new JMenu("Ayuda");
        menuAyuda.add(item("Acerca de"));
        add(menuAyuda);

        botonCompilar = crearBoton("Compilar");
        add(botonCompilar);

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
    public void setOnGuardarComo(ActionListener l) { anclar(4, l); }
    public void setOnDescargarProyecto(ActionListener l) { anclar(6, l); }
    public void setOnSalir(ActionListener l) { anclar(8, l); }
    public void setOnVerErrores(ActionListener l) { anclarReportes(0, l); }
    public void setOnVerSimbolos(ActionListener l) { anclarReportes(1, l); }
    public void setOnVerCuartetas(ActionListener l) { anclarReportes(2, l); }
    public void setOnVerC3D(ActionListener l) { anclarReportes(3, l); }
    public void setOnVerCodigoC(ActionListener l) { anclarReportes(4, l); }
    public void setOnAcercaDe(ActionListener l) { anclarAyuda(0, l); }
    public void setOnCompilar(ActionListener l) { botonCompilar.addActionListener(l); }
    public void setOnLimpiarLog(ActionListener l) { botonLimpiarLog.addActionListener(l); }

    private void anclar(int indice, ActionListener l) {
        JMenu menu = (JMenu) getComponent(0);
        ((JMenuItem) menu.getMenuComponent(indice)).addActionListener(l);
    }

    private void anclarReportes(int indice, ActionListener l) {
        JMenu menu = (JMenu) getComponent(1);
        ((JMenuItem) menu.getMenuComponent(indice)).addActionListener(l);
    }

    private void anclarAyuda(int indice, ActionListener l) {
        JMenu menu = (JMenu) getComponent(2);
        ((JMenuItem) menu.getMenuComponent(indice)).addActionListener(l);
    }
}
