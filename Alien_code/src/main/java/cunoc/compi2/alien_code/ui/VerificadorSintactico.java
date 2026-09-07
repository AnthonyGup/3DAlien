package cunoc.compi2.alien_code.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class VerificadorSintactico {

    public static class ErrorSintactico {
        public final int linea;
        public final int columna;
        public final String mensaje;

        public ErrorSintactico(int linea, int columna, String mensaje) {
            this.linea = linea;
            this.columna = columna;
            this.mensaje = mensaje;
        }
    }

    public static class Resultado {
        public final boolean extensionValida;
        public final String lenguaje;
        public final int cantidadTokens;
        public final List<ErrorSintactico> errores;

        public Resultado(boolean extensionValida, String lenguaje, int cantidadTokens, List<ErrorSintactico> errores) {
            this.extensionValida = extensionValida;
            this.lenguaje = lenguaje;
            this.cantidadTokens = cantidadTokens;
            this.errores = errores;
        }

        public boolean hayErrores() {
            return !errores.isEmpty();
        }
    }

    public Resultado verificar(String codigo, File archivo) {
        String nombre = archivo == null ? "" : archivo.getName().toLowerCase();
        if (nombre.endsWith(".y")) return verificarY(codigo);
        if (nombre.endsWith(".z")) return verificarZ(codigo);
        if (nombre.endsWith(".pig")) return verificarPig(codigo);
        return new Resultado(false, "", 0, new ArrayList<>());
    }

    private Resultado verificarY(String codigo) {
        cunoc.compi2.alien_code.grammar.YLangLexer lexer =
                new cunoc.compi2.alien_code.grammar.YLangLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cunoc.compi2.alien_code.grammar.YLangParser parser =
                new cunoc.compi2.alien_code.grammar.YLangParser(tokens);
        List<ErrorSintactico> errores = analizar(lexer, parser, tokens);
        if (errores.isEmpty()) {
            parser.reset();
            parser.programa();
        }
        return new Resultado(true, "Y?", contarTokens(tokens), errores);
    }

    private Resultado verificarZ(String codigo) {
        cunoc.compi2.alien_code.grammar.ZetarianoLexer lexer =
                new cunoc.compi2.alien_code.grammar.ZetarianoLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cunoc.compi2.alien_code.grammar.ZetarianoParser parser =
                new cunoc.compi2.alien_code.grammar.ZetarianoParser(tokens);
        List<ErrorSintactico> errores = analizar(lexer, parser, tokens);
        if (errores.isEmpty()) {
            parser.reset();
            parser.programa();
        }
        return new Resultado(true, "Zetariano", contarTokens(tokens), errores);
    }

    private Resultado verificarPig(String codigo) {
        cunoc.compi2.alien_code.grammar.PigLatinLexer lexer =
                new cunoc.compi2.alien_code.grammar.PigLatinLexer(CharStreams.fromString(codigo));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        cunoc.compi2.alien_code.grammar.PigLatinParser parser =
                new cunoc.compi2.alien_code.grammar.PigLatinParser(tokens);
        List<ErrorSintactico> errores = analizar(lexer, parser, tokens);
        if (errores.isEmpty()) {
            parser.reset();
            parser.programa();
        }
        return new Resultado(true, "Pig Latin", contarTokens(tokens), errores);
    }

    private List<ErrorSintactico> analizar(Lexer lexer, Recognizer<?, ?> parser, CommonTokenStream tokens) {
        List<ErrorSintactico> errores = new ArrayList<>();
        BaseErrorListener escucha = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> reconocedor, Object simbolo, int linea, int columna,
                    String mensaje, RecognitionException causa) {
                errores.add(new ErrorSintactico(linea, columna + 1, mensaje));
            }
        };
        lexer.removeErrorListeners();
        lexer.addErrorListener(escucha);
        parser.removeErrorListeners();
        parser.addErrorListener(escucha);
        tokens.fill();
        return errores;
    }

    private int contarTokens(CommonTokenStream tokens) {
        int cantidad = tokens.getTokens().size();
        return cantidad > 0 ? cantidad - 1 : 0;
    }
}