package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

public class PanelArbolProyecto extends JPanel {

    private File raizProyecto;
    private final JTree arbol;
    private final DefaultTreeModel modelo;
    private final DefaultMutableTreeNode nodoRaiz;

    public PanelArbolProyecto() {
        setLayout(new BorderLayout());

        nodoRaiz = new DefaultMutableTreeNode("Proyecto");
        modelo = new DefaultTreeModel(nodoRaiz);
        arbol = new JTree(modelo);
        arbol.setRootVisible(true);
        arbol.setCellRenderer(new RendererArbol());
        arbol.setShowsRootHandles(true);

        arbol.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    abrirNodoDobleClic();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    mostrarMenuContextual(e);
                }
            }
        });

        add(new JScrollPane(arbol), BorderLayout.CENTER);
    }

    public void setRaizProyecto(File raiz) {
        this.raizProyecto = raiz;
        recargar();
    }

    public void recargar() {
        nodoRaiz.removeAllChildren();
        if (raizProyecto != null && raizProyecto.isDirectory()) {
            nodoRaiz.setUserObject(raizProyecto.getName());
            for (File hijo : listarArchivos(raizProyecto)) {
                nodoRaiz.add(construirNodo(hijo));
            }
        } else {
            nodoRaiz.setUserObject("Proyecto");
        }
        modelo.reload();
        expandir(nodoRaiz);
    }

    private DefaultMutableTreeNode construirNodo(File archivo) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(archivo);
        if (archivo.isDirectory()) {
            for (File hijo : listarArchivos(archivo)) {
                nodo.add(construirNodo(hijo));
            }
        }
        return nodo;
    }

    private File[] listarArchivos(File dir) {
        File[] archivos = dir.listFiles();
        if (archivos == null) {
            return new File[0];
        }
        java.util.Arrays.sort(archivos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return archivos;
    }

    private void expandir(DefaultMutableTreeNode nodo) {
        TreePath ruta = new TreePath(nodo.getPath());
        arbol.expandPath(ruta);
    }

    private void abrirNodoDobleClic() {
        TreePath ruta = arbol.getSelectionPath();
        if (ruta == null) return;
        Object obj = ((DefaultMutableTreeNode) ruta.getLastPathComponent()).getUserObject();
        if (obj instanceof File f && f.isFile()) {
            if (listenerArchivo != null) {
                listenerArchivo.abrirArchivo(f);
            }
        }
    }

    public File getArchivoSeleccionado() {
        TreePath ruta = arbol.getSelectionPath();
        if (ruta == null) return null;
        Object obj = ((DefaultMutableTreeNode) ruta.getLastPathComponent()).getUserObject();
        return obj instanceof File f ? f : null;
    }

    private void mostrarMenuContextual(java.awt.event.MouseEvent e) {
        TreePath ruta = arbol.getPathForLocation(e.getX(), e.getY());
        if (ruta != null) {
            arbol.setSelectionPath(ruta);
        }

        JPopupMenu menu = new JPopupMenu();
        JMenuItem nuevoArchivo = new JMenuItem("Nuevo archivo");
        nuevoArchivo.addActionListener(ev -> nuevoArchivo());
        menu.add(nuevoArchivo);

        JMenuItem nuevaCarpeta = new JMenuItem("Nueva carpeta");
        nuevaCarpeta.addActionListener(ev -> nuevaCarpeta());
        menu.add(nuevaCarpeta);

        menu.addSeparator();

        JMenuItem renombrar = new JMenuItem("Renombrar");
        renombrar.addActionListener(ev -> renombrar());
        menu.add(renombrar);

        JMenuItem eliminar = new JMenuItem("Eliminar");
        eliminar.addActionListener(ev -> eliminar());
        menu.add(eliminar);

        menu.show(arbol, e.getX(), e.getY());
    }

    private File getCarpetaDestino() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado != null) {
            return seleccionado.isDirectory() ? seleccionado : seleccionado.getParentFile();
        }
        return raizProyecto;
    }

    private void nuevoArchivo() {
        File dir = getCarpetaDestino();
        if (dir == null) {
            JOptionPane.showMessageDialog(this, "Abre una carpeta de proyecto primero.");
            return;
        }
        File archivo = mostrarDialogoNuevoArchivo(this, dir);
        if (archivo == null) return;
        try {
            if (archivo.createNewFile() && listenerArchivo != null) {
                listenerArchivo.abrirArchivo(archivo);
            }
            recargar();
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static File mostrarDialogoNuevoArchivo(Component padre, File carpeta) {
        JTextField campoNombre = new JTextField();
        JComboBox<String> comboExtension = new JComboBox<>(new String[]{".y", ".z", ".pig"});
        comboExtension.setSelectedItem(".pig");
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Nombre del archivo:"));
        panel.add(campoNombre);
        panel.add(new JLabel("Extensión (si no la escribes en el nombre):"));
        panel.add(comboExtension);
        int opcion = JOptionPane.showConfirmDialog(padre, panel, "Nuevo archivo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return null;
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) return null;
        if (!nombre.contains(".")) {
            nombre = nombre + comboExtension.getSelectedItem();
        }
        return new File(carpeta, nombre);
    }

    private void nuevaCarpeta() {
        File dir = getCarpetaDestino();
        if (dir == null) {
            JOptionPane.showMessageDialog(this, "Abre una carpeta de proyecto primero.");
            return;
        }
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la carpeta:", "Nueva carpeta", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null || nombre.isBlank()) return;
        File carpeta = new File(dir, nombre);
        if (carpeta.mkdirs()) {
            recargar();
        }
    }

    private void renombrar() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo o carpeta.");
            return;
        }
        String nuevo = JOptionPane.showInputDialog(this, "Nuevo nombre:", seleccionado.getName());
        if (nuevo == null || nuevo.isBlank()) return;
        File destino = new File(seleccionado.getParentFile(), nuevo);
        if (seleccionado.renameTo(destino)) {
            recargar();
        }
    }

    private void eliminar() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado == null) return;
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + seleccionado.getName() + "\"?",
                "Eliminar", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            eliminarRecursivo(seleccionado);
            recargar();
        }
    }

    private void eliminarRecursivo(File archivo) {
        if (archivo.isDirectory()) {
            File[] hijos = archivo.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) {
                    eliminarRecursivo(hijo);
                }
            }
        }
        archivo.delete();
    }

    public interface ListenerArchivo {
        void abrirArchivo(File archivo);
    }

    private ListenerArchivo listenerArchivo;

    public void setListenerArchivo(ListenerArchivo listener) {
        this.listenerArchivo = listener;
    }

    private static class RendererArbol extends DefaultTreeCellRenderer {

        private static final Color COLOR_YLANG = new Color(220, 60, 60);
        private static final Color COLOR_ZETARIANO = new Color(60, 120, 220);
        private static final Color COLOR_PIGLATIN = new Color(60, 180, 100);
        private static final Color COLOR_CARPETA = new Color(200, 160, 60);

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode nodo) {
                Object obj = nodo.getUserObject();
                if (obj instanceof File f) {
                    setIcon(iconoPara(f));
                }
            }
            return this;
        }

        private Icon iconoPara(File f) {
            return new IconoColor(12, f.isDirectory() ? COLOR_CARPETA
                    : colorSegunExtension(f.getName()));
        }

        private Color colorSegunExtension(String nombre) {
            String n = nombre.toLowerCase();
            if (n.endsWith(".z")) return COLOR_ZETARIANO;
            if (n.endsWith(".pig")) return COLOR_PIGLATIN;
            return COLOR_YLANG;
        }
    }

    private static class IconoColor implements Icon {
        private final int tamano;
        private final Color color;

        IconoColor(int tamano, Color color) {
            this.tamano = tamano;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRoundRect(x, y, tamano, tamano, 4, 4);
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }
}
