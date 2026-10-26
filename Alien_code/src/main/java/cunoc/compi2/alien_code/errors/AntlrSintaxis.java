package cunoc.compi2.alien_code.errors;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Convierte los errores de sintaxis de ANTLR en CompilerError (tipo SINTACTICO)
 * para que la pipeline pueda abortar en lugar de ejecutar codigo recuperado.
 */
public final class AntlrSintaxis {

    private AntlrSintaxis() {
    }

    public static ANTLRErrorListener hacia(ErrorListener destino) {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                    int line, int charPositionInLine, String msg, RecognitionException e) {
                destino.addError(ErrorType.SINTACTICO, msg, line, charPositionInLine + 1);
            }
        };
    }
}
