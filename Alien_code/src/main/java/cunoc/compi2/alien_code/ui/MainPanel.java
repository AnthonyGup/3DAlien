package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

public class MainPanel extends JPanel {

    private final PanelArbolProyecto arbolProyecto;
    private final PanelEditorTabs editorTabs;
    private final PanelLog panelLog;

    public MainPanel(PanelArbolProyecto arbolProyecto, PanelEditorTabs editorTabs, PanelLog panelLog) {
        this.arbolProyecto = arbolProyecto;
        this.editorTabs = editorTabs;
        this.panelLog = panelLog;

        setLayout(new BorderLayout());

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, panelLog);
        vertical.setResizeWeight(0.67);
        vertical.setContinuousLayout(true);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, arbolProyecto, vertical);
        horizontal.setResizeWeight(0.25);
        horizontal.setContinuousLayout(true);
        horizontal.setBorder(new EmptyBorder(10, 10, 10, 10));

        horizontal.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                horizontal.setDividerLocation(0.25);
                vertical.setDividerLocation(0.67);
                horizontal.removeComponentListener(this);
            }
        });

        add(horizontal, BorderLayout.CENTER);
    }

    public PanelArbolProyecto getArbolProyecto() {
        return arbolProyecto;
    }

    public PanelEditorTabs getEditorTabs() {
        return editorTabs;
    }

    public PanelLog getPanelLog() {
        return panelLog;
    }
}
