package cunoc.compi2.alien_code.ui;

import javax.swing.text.Segment;
import org.antlr.v4.runtime.CharStreams;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import cunoc.compi2.alien_code.ylang.grammar.YLangLexer;

public class YLangTokenMaker extends AbstractTokenMaker {

    private static final String[] RESERVADAS = {
        "estructura", "definir", "retornar", "si", "entonces", "sino", "contrario",
        "elegir", "caso", "siempre", "romper", "continuar", "para", "mientras", "hacer",
        "entero", "flotante", "cadena", "caracter", "bool", "verdadero", "falso"
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
        YLangLexer lexer = new YLangLexer(CharStreams.fromString(codigo));
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
            case YLangLexer.NUMERO:
                return TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
            case YLangLexer.DECIMAL:
                return TokenTypes.LITERAL_NUMBER_FLOAT;
            case YLangLexer.CADENA:
                return TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
            case YLangLexer.CARACTER:
                return TokenTypes.LITERAL_CHAR;
            case YLangLexer.IDENTIFICADOR:
                if (esReservada(token.getText())) {
                    return TokenTypes.RESERVED_WORD;
                }
                return TokenTypes.IDENTIFIER;
            case YLangLexer.ESTRUCTURAS_MARKER:
            case YLangLexer.FUNCIONES_MARKER:
                return TokenTypes.RESERVED_WORD;
            case YLangLexer.WS_INTERNO:
            case YLangLexer.NEWLINE:
            case YLangLexer.INDENT:
            case YLangLexer.DEDENT:
                return TokenTypes.WHITESPACE;
            case YLangLexer.COMENTARIO_BLOQUE:
                return TokenTypes.COMMENT_MULTILINE;
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
