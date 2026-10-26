package cunoc.compi2.alien_code.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class PanelEditorTabs extends JTabbedPane {

    public PanelEditorTabs() {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public EditorPanel abrirArchivo(File archivo) {
        EditorPanel existente = buscarPorArchivo(archivo);
        if (existente != null) {
            setSelectedComponent(existente);
            return existente;
        }

        EditorPanel editor = new EditorPanel(archivo);
        addTab(archivo.getName(), editor);
        int indice = indexOfComponent(editor);
        setTabComponentAt(indice, crearComponentePestana(editor, archivo.getName()));
        setSelectedComponent(editor);
        setToolTipTextAt(indice, archivo.getAbsolutePath());
        return editor;
    }

    public EditorPanel getEditorSeleccionado() {
        return (EditorPanel) getSelectedComponent();
    }

    public EditorPanel buscarPorArchivo(File archivo) {
        int n = getTabCount();
        for (int i = 0; i < n; i++) {
            EditorPanel editor = (EditorPanel) getComponentAt(i);
            if (editor.getArchivo() != null && editor.getArchivo().equals(archivo)) {
                return editor;
            }
        }
        return null;
    }

    public void cerrar(EditorPanel editor) {
        if (editor == null || indexOfComponent(editor) < 0) {
            return;
        }
        String nombre = editor.getArchivo() != null
                ? editor.getArchivo().getName() : "Sin nombre";

        if (estaModificado(editor)) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "'" + nombre + "' tiene cambios sin guardar.\n¿Guardar antes de cerrar?",
                    "Cerrar archivo", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (opcion == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (opcion == JOptionPane.YES_OPTION) {
                guardar(editor);
            }
        }
        remove(editor);
    }

    private boolean estaModificado(EditorPanel editor) {
        File archivo = editor.getArchivo();
        if (archivo == null || !archivo.isFile()) {
            return !editor.getText().isBlank();
        }
        try {
            String enDisco = java.nio.file.Files.readString(archivo.toPath());
            return !enDisco.equals(editor.getText());
        } catch (IOException e) {
            return false;
        }
    }

    private void guardar(EditorPanel editor) {
        File archivo = editor.getArchivo();
        if (archivo == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(archivo.toPath(), editor.getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el archivo.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel crearComponentePestana(EditorPanel editor, String nombre) {
        JPanel pestana = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pestana.setOpaque(false);

        JLabel etiqueta = new JLabel(nombre);
        pestana.add(etiqueta);

        JButton cerrar = new JButton("x");
        cerrar.setFont(cerrar.getFont().deriveFont(Font.PLAIN, 11f));
        cerrar.setMargin(new Insets(0, 4, 0, 2));
        cerrar.setFocusable(false);
        cerrar.setOpaque(false);
        cerrar.setContentAreaFilled(false);
        cerrar.setBorderPainted(false);
        cerrar.setBorder(BorderFactory.createEmptyBorder());
        cerrar.setToolTipText("Cerrar " + nombre);
        cerrar.addActionListener(e -> cerrar(editor));
        pestana.add(cerrar);

        return pestana;
    }
}
