package cunoc.compi2.alien_code.zetariano.semantic;

import cunoc.compi2.alien_code.ast.Type;

public final class ZetarianoVocabulary {

    private ZetarianoVocabulary() {
    }

    public static Type tipoDe(String nombre) {
        return Type.fromZetariano(nombre);
    }
}