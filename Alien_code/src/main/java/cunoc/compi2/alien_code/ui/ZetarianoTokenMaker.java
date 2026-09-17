package cunoc.compi2.alien_code.ui;

import javax.swing.text.Segment;
import org.antlr.v4.runtime.CharStreams;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import cunoc.compi2.alien_code.zetariano.grammar.ZetarianoLexer;

public class ZetarianoTokenMaker extends AbstractTokenMaker {

    private static final String[] RESERVADAS = {
        "class", "public", "void", "new", "null", "true", "false",
        "if", "else", "switch", "case", "default", "break", "continue",
        "for", "while", "do", "return", "int", "double", "char", "boolean", "String"
    };

    @Override
    public TokenMap getWordsToHighlight() {
        return null;
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int finLinea = offset + count - 1;

        String linea = new String(array, offset, count);
        int pos = 0;

        while (pos < count) {
            int apertura = linea.indexOf("//", pos);
            String codigo = apertura < 0 ? linea.substring(pos) : linea.substring(pos, apertura);
            if (!codigo.isEmpty()) {
                agregarTokensAntlr(array, offset, startOffset, pos, codigo);
            }
            if (apertura < 0) {
                break;
            }
            addToken(array, offset + apertura, finLinea, TokenTypes.COMMENT_EOL, startOffset + apertura);
            pos = count;
        }

        addNullToken();
        return firstToken;
    }

    private void agregarTokensAntlr(char[] array, int offset, int startOffset, int desplazamiento, String codigo) {
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(codigo));
        lexer.removeErrorListeners();

        for (org.antlr.v4.runtime.Token token : lexer.getAllTokens()) {
            int inicio = offset + desplazamiento + token.getStartIndex();
            int fin = offset + desplazamiento + token.getStopIndex();
            addToken(array, inicio, fin, mapearTipo(token), startOffset + desplazamiento + token.getStartIndex());
        }
    }

    private int mapearTipo(org.antlr.v4.runtime.Token token) {
        int tipo = token.getType();
        switch (tipo) {
            case ZetarianoLexer.NUMERO:
                return TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
            case ZetarianoLexer.DECIMAL:
                return TokenTypes.LITERAL_NUMBER_FLOAT;
            case ZetarianoLexer.CADENA:
                return TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
            case ZetarianoLexer.CARACTER:
                return TokenTypes.LITERAL_CHAR;
            case ZetarianoLexer.IDENTIFICADOR:
                return TokenTypes.IDENTIFIER;
            case ZetarianoLexer.WS:
                return TokenTypes.WHITESPACE;
            default:
                if (esReservada(token.getText())) {
                    return TokenTypes.RESERVED_WORD;
                }
                return TokenTypes.OPERATOR;
        }
    }

    private boolean esReservada(String texto) {
        for (String r : RESERVADAS) {
            if (r.equals(texto)) {
                return true;
            }
        }
        return false;
    }
}
