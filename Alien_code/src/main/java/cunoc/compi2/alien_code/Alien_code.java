package cunoc.compi2.alien_code;

import cunoc.compi2.alien_code.errors.ErrorListener;

public class Alien_code {

    private final ErrorListener errorListener;

    public Alien_code() {
        this.errorListener = new ErrorListener();
    }

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public static void main(String[] args) {
        System.out.println("=== 3DAlien Compiler ===");
        System.out.println("Lenguajes soportados: Y?, Zetariano, Pig Latin");
    }
}
