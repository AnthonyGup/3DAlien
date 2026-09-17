lexer grammar YLangLexer;

tokens { INDENT, DEDENT }

@members {
    private final java.util.LinkedList<Integer> indentStack = new java.util.LinkedList<>();
    private final java.util.LinkedList<Token> tokenQueue = new java.util.LinkedList<>();
    private int opened = 0;
    private boolean eofHandled = false;

    { indentStack.push(0); }

    @Override
    public Token nextToken() {
        if (!tokenQueue.isEmpty()) {
            return tokenQueue.poll();
        }

        Token next = super.nextToken();

        if (next.getType() == PAREN_IZQ || next.getType() == CORCHETE_IZQ) {
            opened++;
        } else if (next.getType() == PAREN_DER || next.getType() == CORCHETE_DER) {
            opened--;
        }

        if (next.getType() == Token.EOF) {
            if (!eofHandled) {
                eofHandled = true;
                while (indentStack.peek() != 0) {
                    indentStack.pop();
                    tokenQueue.offer(fabricarToken(DEDENT));
                }
            }
            tokenQueue.offer(next);
            return tokenQueue.poll();
        }

        if (next.getType() != NEWLINE || opened > 0) {
            return next;
        }

        String texto = next.getText();
        int indent = 0;
        for (int i = texto.length() - 1; i >= 0 && (texto.charAt(i) == ' ' || texto.charAt(i) == '\t'); i--) {
            indent++;
        }

        int nivelActual = indentStack.peek();
        if (indent > nivelActual) {
            indentStack.push(indent);
            tokenQueue.offer(next);
            tokenQueue.offer(fabricarToken(INDENT));
        } else if (indent < nivelActual) {
            tokenQueue.offer(next);
            while (indentStack.peek() > indent) {
                indentStack.pop();
                tokenQueue.offer(fabricarToken(DEDENT));
            }
        } else {
            tokenQueue.offer(next);
        }

        return tokenQueue.poll();
    }

    private Token fabricarToken(int tipo) {
        CommonToken t = new CommonToken(tipo, tipo == INDENT ? "<INDENT>" : "<DEDENT>");
        t.setLine(getLine());
        t.setCharPositionInLine(getCharPositionInLine());
        return t;
    }
}

// ── Marcadores de sección ────────────────────────────────────

ESTRUCTURAS_MARKER : '%estructuras';
FUNCIONES_MARKER    : '%funciones';

// ── Palabras reservadas ──────────────────────────────────────

ESTRUCTURA    : 'estructura';
DEFINIR       : 'definir';
RETORNAR      : 'retornar';
SI            : 'si';
ENTONCES      : 'entonces';
SINO          : 'sino';
CONTRARIO     : 'contrario';
ELEGIR        : 'elegir';
CASO          : 'caso';
SIEMPRE       : 'siempre';
ROMPER        : 'romper';
CONTINUAR     : 'continuar';
PARA          : 'para';
MIENTRAS      : 'mientras';
HACER         : 'hacer';
ENTERO        : 'entero';
FLOTANTE      : 'flotante';
CADENA_TIPO   : 'cadena';
CARACTER_TIPO : 'caracter';
BOOL          : 'bool';
VERDADERO     : 'verdadero';
FALSO         : 'falso';

// ── Operadores (los de dos caracteres primero) ───────────────

OR          : '||';
AND         : '&&';
IGUAL       : '==';
DISTINTO    : '!=';
INCREMENTO  : '++';
DECREMENTO  : '--';
FLECHA      : '->';
NEGACION    : '!';
MENOR       : '<';
MAYOR       : '>';
ASIGNACION  : '=';
MAS         : '+';
MENOS       : '-';
POR         : '*';
DIV         : '/';

// ── Símbolos ─────────────────────────────────────────────────

CORCHETES_VACIOS : '[]';
LLAVES_VACIAS    : '{}';
PAREN_IZQ    : '(';
PAREN_DER    : ')';
LLAVE_IZQ    : '{';
LLAVE_DER    : '}';
CORCHETE_IZQ : '[';
CORCHETE_DER : ']';
DOS_PUNTOS   : ':';
PUNTO_COMA   : ';';
PUNTO        : '.';
COMA         : ',';

// ── Literales ────────────────────────────────────────────────

NUMERO        : [0-9]+;
DECIMAL       : NUMERO '.' NUMERO;
CADENA        : '"' ~'"'* '"';
CARACTER      : '\'' ~'\'' '\'';
IDENTIFICADOR : [a-zA-Z_][a-zA-Z0-9_]*;

// ── Comentarios y espacios ───────────────────────────────────

COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN);
COMENTARIO_BLOQUE : '/*' .*? '*/' -> channel(HIDDEN);
WS_INTERNO        : [ \t]+ -> channel(HIDDEN);

NEWLINE
    : ( '\r'? '\n' | '\r' )
      ( [ \t]* ( '//' ~[\r\n]* | '/*' .*? '*/' )? ( '\r'? '\n' | '\r' ) )*
      [ \t]*
    ;