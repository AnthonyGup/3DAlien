package cunoc.compi2.alien_code.ui;

import javax.swing.text.Segment;
import org.antlr.v4.runtime.CharStreams;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import cunoc.compi2.alien_code.grammar.PigLatinLexer;

public class PigLatinTokenMaker extends AbstractTokenMaker {

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
        boolean enComentarioBloque = initialTokenType == TokenTypes.COMMENT_MULTILINE;
        int pos = 0;

        while (pos < count) {
            if (enComentarioBloque) {
                int cierre = linea.indexOf("##", pos);
                if (cierre < 0) {
                    addToken(array, offset + pos, finLinea, TokenTypes.COMMENT_MULTILINE, startOffset + pos);
                    break;
                }
                addToken(array, offset + pos, offset + cierre + 1, TokenTypes.COMMENT_MULTILINE, startOffset + pos);
                pos = cierre + 2;
                enComentarioBloque = false;
                continue;
            }

            int aperturaBloque = linea.indexOf("##", pos);
            int aperturaLinea = linea.indexOf("//", pos);
            int apertura;
            if (aperturaLinea < 0) {
                apertura = aperturaBloque;
            } else if (aperturaBloque < 0) {
                apertura = aperturaLinea;
            } else {
                apertura = Math.min(aperturaLinea, aperturaBloque);
            }

            String codigo = apertura < 0 ? linea.substring(pos) : linea.substring(pos, apertura);
            if (!codigo.isEmpty()) {
                agregarTokensAntlr(array, offset, startOffset, pos, codigo);
            }
            if (apertura < 0) {
                break;
            }

            if (apertura == aperturaLinea) {
                addToken(array, offset + apertura, finLinea, TokenTypes.COMMENT_EOL, startOffset + apertura);
                pos = count;
            } else {
                int cierre = linea.indexOf("##", apertura + 2);
                if (cierre < 0) {
                    addToken(array, offset + apertura, finLinea, TokenTypes.COMMENT_MULTILINE, startOffset + apertura);
                    break;
                }
                addToken(array, offset + apertura, offset + cierre + 1, TokenTypes.COMMENT_MULTILINE, startOffset + apertura);
                pos = cierre + 2;
            }
        }

        addNullToken();
        return firstToken;
    }

    private void agregarTokensAntlr(char[] array, int offset, int startOffset, int desplazamiento, String codigo) {
        PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(codigo));
        lexer.removeErrorListeners();

        for (org.antlr.v4.runtime.Token token : lexer.getAllTokens()) {
            int inicio = offset + desplazamiento + token.getStartIndex();
            int fin = offset + desplazamiento + token.getStopIndex();
            addToken(array, inicio, fin, mapearTipo(token.getType()), startOffset + desplazamiento + token.getStartIndex());
        }
    }

    private int mapearTipo(int tipoAntlr) {
        switch (tipoAntlr) {
            case PigLatinLexer.NUMERO:
                return TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
            case PigLatinLexer.DECIMAL:
                return TokenTypes.LITERAL_NUMBER_FLOAT;
            case PigLatinLexer.CADENA:
                return TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
            case PigLatinLexer.CARACTER:
                return TokenTypes.LITERAL_CHAR;
            case PigLatinLexer.VERUM:
            case PigLatinLexer.FALSUS:
                return TokenTypes.LITERAL_BOOLEAN;
            case PigLatinLexer.NUMERUS:
            case PigLatinLexer.DECIMALIS:
            case PigLatinLexer.TEXTUM:
            case PigLatinLexer.LITTERA:
            case PigLatinLexer.BOOL:
                return TokenTypes.DATA_TYPE;
            case PigLatinLexer.IMPORT:
            case PigLatinLexer.ESTO:
            case PigLatinLexer.SERIES:
            case PigLatinLexer.NOVUS:
            case PigLatinLexer.FINIS:
            case PigLatinLexer.PROGRAMA_FIN:
            case PigLatinLexer.SI:
            case PigLatinLexer.ALITER:
            case PigLatinLexer.DUM:
            case PigLatinLexer.FACERE:
            case PigLatinLexer.PER:
            case PigLatinLexer.PERGE:
            case PigLatinLexer.INTERRUMPE:
            case PigLatinLexer.NON:
                return TokenTypes.RESERVED_WORD;
            case PigLatinLexer.VARIABILES_MARKER:
            case PigLatinLexer.MAIOR_MARKER:
                return TokenTypes.PREPROCESSOR;
            case PigLatinLexer.IDENTIFICADOR:
                return TokenTypes.IDENTIFIER;
            case PigLatinLexer.WS:
                return TokenTypes.WHITESPACE;
            default:
                return TokenTypes.OPERATOR;
        }
    }
}