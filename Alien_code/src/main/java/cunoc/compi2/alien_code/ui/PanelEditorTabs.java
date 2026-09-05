package cunoc.compi2.alien_code.ui;

import java.io.File;
import javax.swing.JTabbedPane;

public class PanelEditorTabs extends JTabbedPane {

    public EditorPanel abrirArchivo(File archivo) {
        EditorPanel existente = buscarPorArchivo(archivo);
        if (existente != null) {
            setSelectedComponent(existente);
            return existente;
        }

        EditorPanel editor = new EditorPanel(archivo);
        addTab(archivo.getName(), editor);
        setSelectedComponent(editor);
        setToolTipTextAt(indexOfComponent(editor), archivo.getAbsolutePath());
        return editor;
    }

    public EditorPanel crearNuevaPestana(String titulo) {
        EditorPanel editor = new EditorPanel();
        addTab(titulo, editor);
        setSelectedComponent(editor);
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

    public void cerrarPestanaSeleccionada() {
        int index = getSelectedIndex();
        if (index >= 0) {
            removeTabAt(index);
        }
    }
}
