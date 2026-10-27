package cunoc.compi2.alien_code.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MainPanel extends JPanel {

    private static final int PESTANA_LOG = 0;
    private static final int PESTANA_RISCV = 1;

    private final PanelArbolProyecto arbolProyecto;
    private final PanelEditorTabs editorTabs;
    private final PanelLog panelLog;
    private final PanelCodigoRISCV panelRiscV;
    private final JTabbedPane pestanasInferiores;

    public MainPanel(PanelArbolProyecto arbolProyecto, PanelEditorTabs editorTabs,
            PanelLog panelLog, PanelCodigoRISCV panelRiscV) {
        this.arbolProyecto = arbolProyecto;
        this.editorTabs = editorTabs;
        this.panelLog = panelLog;
        this.panelRiscV = panelRiscV;

        setLayout(new BorderLayout());

        pestanasInferiores = new JTabbedPane();
        pestanasInferiores.addTab("Log", panelLog);
        pestanasInferiores.addTab("RISC-V", panelRiscV);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, pestanasInferiores);
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

    public void mostrarPestanaLog() {
        pestanasInferiores.setSelectedIndex(PESTANA_LOG);
    }

    public void mostrarPestanaRiscV() {
        pestanasInferiores.setSelectedIndex(PESTANA_RISCV);
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

    public PanelCodigoRISCV getPanelRiscV() {
        return panelRiscV;
    }
}
