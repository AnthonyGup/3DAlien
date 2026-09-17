package cunoc.compi2.alien_code.ylang.semantic;

import cunoc.compi2.alien_code.ast.Type;

public final class YLangVocabulary {

    private YLangVocabulary() {
    }

    public static Type tipoDe(String nombre) {
        return Type.fromYLang(nombre);
    }
}