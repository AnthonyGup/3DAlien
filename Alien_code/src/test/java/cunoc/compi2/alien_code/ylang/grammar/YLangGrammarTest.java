package cunoc.compi2.alien_code.ylang.grammar;

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

class YLangGrammarTest {

    @Test
    void programaMinimoDelSpec() {
        String codigo = """
                %estructuras
                estructura Persona:
                    entero edad
                    cadena nombre
                    flotante promedio

                %funciones
                definir calcularPoder(entero fuerza) -> entero:
                    retornar fuerza * 10
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void programaCompletoConTodasLasConstrucciones() {
        String codigo = """
                %estructuras
                estructura Punto:
                    entero x
                    entero y
                    flotante promedio

                %funciones
                definir principal() -> entero:
                    entero total = 0
                    entero numeros[3] = {-1, 4, 9}
                    flotante peso = 1.5
                    cadena mensaje = "hola"
                    Punto origen = {0, 0, 0.0}
                    total = total + numeros[0]
                    total++
                    total--
                    origen.x = origen.x + 1
                    si (total == 0) entonces
                        imprimir("cero")
                    sino (total > 5) entonces
                        total = total * 2
                    contrario
                        total = total - 1
                    elegir (total):
                        caso 1:
                            total = total + 100
                        siempre:
                            total = 0
                    para (entero i = 0; i < 3; i++):
                        si (i == 2) entonces
                            continuar
                        total = total + numeros[i]
                        romper
                    mientras (total < 10) hacer
                        total = total + 1
                    hacer:
                        total = total - 1
                    mientras (total > 5)
                    retornar total
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void condicionalConSinoYContrarioComoSentencia() {
        String codigo = """
                %funciones
                definir clasificar(entero edad):
                    si (edad > 18) entonces
                        imprimir("mayor")
                    sino (edad == 18) entonces
                        imprimir("igual")
                    contrario
                        imprimir("menor")
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void ciclosAnidadosConComentariosYLinasEnBlanco() {
        String codigo = """
                %funciones
                definir principal():
                    entero i = 0
                    entero j = 0

                    // comentario de linea
                    mientras (i < 3) hacer
                        /* comentario
                           de bloque */
                        j = 0
                        mientras (j < 3) hacer
                            i = i + 1
                            j = j + 1

                    /* comentario final */
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void incrementoYDecrementoComoSentencia() {
        String codigo = """
                %funciones
                definir principal():
                    entero intentos = 0
                    intentos++
                    intentos--
                """;
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void eofSinSaltoDeLineaFinal() {
        String codigo = """
                %funciones
                definir principal():
                    entero i = 0
                    i++""";
        assertTrue(parsearSinErrores(codigo));
    }

    @Test
    void generaTokensIndentYDedentBalanceados() {
        String codigo = """
                %funciones
                definir principal():
                    entero i = 0
                    mientras (i < 2) hacer:
                        i = i + 1
                    retornar i
                """;
        YLangLexer lexer = new YLangLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        int indents = 0;
        int dedents = 0;
        for (Token token : tokens.getTokens()) {
            if (token.getType() == YLangLexer.INDENT) indents++;
            if (token.getType() == YLangLexer.DEDENT) dedents++;
        }
        assertTrue(indents > 0);
        assertEquals(indents, dedents);
    }

    @Test
    void seccionFuncionesEsObligatoria() {
        String codigo = """
                %estructuras
                estructura Persona:
                    entero edad
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    @Test
    void operadoresMenorIgualNoExistenEnY() {
        String codigo = """
                %funciones
                definir principal():
                    entero i = 0
                    mientras (i <= 3) hacer
                        i = i + 1
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    @Test
    void dedentInconsistenteEsError() {
        String codigo = """
                %funciones
                definir principal():
                    entero a = 1
                  entero b = 2
                """;
        assertFalse(parsearSinErrores(codigo));
    }

    private boolean parsearSinErrores(String codigo) {
        YLangLexer lexer = new YLangLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ContadorErrores errores = new ContadorErrores();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errores);

        YLangParser parser = new YLangParser(tokens);
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