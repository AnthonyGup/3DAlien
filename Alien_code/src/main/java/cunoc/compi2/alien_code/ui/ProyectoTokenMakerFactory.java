package cunoc.compi2.alien_code.ui;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;

public class ProyectoTokenMakerFactory extends AbstractTokenMakerFactory {

    public static final String LENGUAJE_YLANG = "text/ylang";
    public static final String LENGUAJE_ZETARIANO = "text/zetariano";
    public static final String LENGUAJE_PIGLATIN = "text/piglatin";

    @Override
    protected void initTokenMakerMap() {
        putMapping(LENGUAJE_YLANG, "cunoc.compi2.alien_code.ui.YLangTokenMaker");
        putMapping(LENGUAJE_ZETARIANO, "cunoc.compi2.alien_code.ui.ZetarianoTokenMaker");
        putMapping(LENGUAJE_PIGLATIN, "cunoc.compi2.alien_code.ui.PigLatinTokenMaker");
    }
}
