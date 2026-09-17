package cunoc.compi2.alien_code.pigLatin.semantic;

import cunoc.compi2.alien_code.ast.Type;

public final class PigLatinVocabulary {

    private PigLatinVocabulary() {
    }

    public static Type tipoDe(String nombre) {
        return Type.fromPigLatin(nombre);
    }
}