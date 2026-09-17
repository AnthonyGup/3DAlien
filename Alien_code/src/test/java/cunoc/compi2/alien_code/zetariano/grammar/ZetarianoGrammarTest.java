package cunoc.compi2.alien_code.zetariano.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZetarianoGrammarTest {

    @Test
    void claseVaciaSinMiembros() {
        String codigo = """
                public class Vacio {
                }
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void programaCompletoConTodasLasConstrucciones() {
        String codigo = """
                public class Persona {
                    int edad;
                    double promedio;
                    String nombre;
                    boolean activo;
                    char inicial;
                    int[] numeros;

                    public Persona(String nombre) {
                        this.nombre = nombre;
                    }

                    public int calcularPoder() {
                        int total = 10;
                        total += 5;
                        total -= 2;
                        total *= 3;
                        total++;
                        total--;
                        if (total > 100) {
                            total = 100;
                        } else if (total < 0) {
                            total = 0;
                        } else {
                            total = total % 2;
                        }
                        switch (total) {
                            case 1:
                                break;
                            default:
                                continue;
                        }
                        for (int i = 0; i < 3; i++) {
                            numeros[i] = i;
                        }
                        int j = 0;
                        while (j < 3) {
                            j++;
                        }
                        do {
                            j--;
                        } while (j > 0);
                        return total > 5 ? total : 5;
                    }

                    public void imprimirAll(int[] datos) {
                        System.out.println(datos[0]);
                    }

                    public void main(String[] args) {
                        Persona p = new Persona("x");
                        int[] numeros = new int[5];
                        int[][] matriz = new int[3][3];
                        int[] lista = {1, 2, 3};
                        boolean b = (true && false) || !false;
                        if (b == false && matriz[0][0] == 0) {
                            continue;
                        }
                    }
                }
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void cuerpoDeControlSinLlavesPermitido() {
        String codigo = """
                public class UnoCuerpo {
                    public void correr() {
                        for (int i = 0; i < 3; i++)
                            i = i + 1;
                        if (i == 1)
                            break;
                        else
                            i = 0;
                        while (i < 3)
                            i++;
                        do
                            i--;
                        while (i > 0);
                    }
                }
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void claseEsObligatoria() {
        String codigo = """
                int x;
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    @Test
    void publicYClassSonObligatorios() {
        String codigo = """
                class SinPublic {
                }
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    @Test
    void faltaPuntoYComaEsError() {
        String codigo = """
                public class Error {
                    public void correr() {
                        int x = 1
                    }
                }
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    @Test
    void eofSinSaltoDeLineaFinal() {
        String codigo = "public class Final {\n    public void fin() {\n        int x = 0;\n    }\n}";
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void numeroYDecimalSeTokenizanSeparados() {
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString("a = 42; b = 3.14;"));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();
        List<Token> tokens = tokenStream.getTokens();

        boolean hayNumero = false;
        boolean hayDecimal = false;
        for (Token token : tokens) {
            if (token.getType() == ZetarianoLexer.NUMERO) hayNumero = true;
            if (token.getType() == ZetarianoLexer.DECIMAL) hayDecimal = true;
        }
        assertTrue(hayNumero);
        assertTrue(hayDecimal);
    }

    @Test
    void arregloDeTipoBaseConDDasDimensiones() {
        String codigo = """
                public class Matriz {
                    public void crear() {
                        int[][] m = new int[3][3];
                        m[0][1] = 5;
                    }
                }
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void literalArregloComoValor() {
        String codigo = """
                public class Datos {
                    public void llenar() {
                        int[] numeros = {1, 2, 3};
                    }
                }
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    private boolean parsearSinErrores(String codigo) {
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ContadorErrores errores = new ContadorErrores();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errores);

        ZetarianoParser parser = new ZetarianoParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errores);

        parser.programa();
        return errores.cantidad == 0;
    }

    private static class ContadorErrores extends BaseErrorListener {
        int cantidad;

        @Override
        public void syntaxError(Recognizer<?, ?> reconocedor, Object simbolo, int linea, int columna,
                String mensaje, RecognitionException causa) {
            cantidad++;
        }
    }
}