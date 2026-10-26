grammar Zetariano;

// ── Programa: una sola clase pública por archivo ─────────────

programa
    : visibilidad? CLASS IDENTIFICADOR (EXTENDS IDENTIFICADOR)? LLAVE_IZQ miembroClase* LLAVE_DER EOF
    ;

visibilidad
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;

miembroClase
    : campoAtributo
    | constructor
    | metodo
    ;

campoAtributo
    : visibilidad? tipo arrayDims? IDENTIFICADOR (ASIGNACION expresion)? PUNTO_COMA
    ;

constructor
    : visibilidad? IDENTIFICADOR PAREN_IZQ parametros? PAREN_DER bloque
    ;

metodo
    : OVERRIDE? visibilidad? (VOID | tipo arrayDims?) IDENTIFICADOR PAREN_IZQ parametros? PAREN_DER bloque
    ;

parametros
    : parametro (COMA parametro)*
    ;

parametro
    : tipo arrayDims? IDENTIFICADOR
    ;

arrayDims
    : (CORCHETE_IZQ CORCHETE_DER)+
    ;

// ── Tipos ────────────────────────────────────────────────────

tipo
    : tipoBase
    | IDENTIFICADOR      // nombre de otra clase (.z)
    ;

tipoBase
    : INT
    | DOUBLE
    | CHAR
    | BOOLEAN
    | STRING
    ;

// ── Bloques y sentencias ──────────────────────────────────────

bloque
    : LLAVE_IZQ sentencia* LLAVE_DER
    ;

// Para if/else/for/while/do: el cuerpo puede ser un bloque {} o una
// única sentencia sin llaves (permitido explícitamente por el spec).
cuerpoOSentencia
    : bloque
    | sentencia
    ;

sentencia
    : declaracionVariable
    | asignacion
    | asignacionCompuesta
    | incremento
    | decremento
    | accesoVariable PUNTO_COMA      // llamada a método/función usada como sentencia
    | condicional
    | seleccion
    | forClasico
    | whileClasico
    | doWhileClasico
    | retorno
    | superLlamada
    | superMetodo
    | BREAK PUNTO_COMA
    | CONTINUE PUNTO_COMA
    | bloque                          // bloque anidado suelto
    ;

declaracionVariable
    : tipo arrayDims? IDENTIFICADOR (ASIGNACION expresion)? PUNTO_COMA
    ;

asignacion
    : accesoVariable ASIGNACION expresion PUNTO_COMA
    ;

asignacionCompuesta
    : accesoVariable (MAS_ASIGNA | MENOS_ASIGNA | POR_ASIGNA) expresion PUNTO_COMA
    ;

incremento
    : accesoVariable INCREMENTO PUNTO_COMA
    ;

decremento
    : accesoVariable DECREMENTO PUNTO_COMA
    ;

retorno
    : RETURN expresion? PUNTO_COMA
    ;

superLlamada
    : SUPER PAREN_IZQ argumentos? PAREN_DER PUNTO_COMA
    ;

superMetodo
    : SUPER PUNTO IDENTIFICADOR PAREN_IZQ argumentos? PAREN_DER PUNTO_COMA
    ;

// ── Condicional (if / else if / else), llaves opcionales ─────

condicional
    : IF PAREN_IZQ expresion PAREN_DER cuerpoOSentencia ramaElse?
    ;

ramaElse
    : ELSE condicional          // else if (...) ...  -> se modela como un IF anidado
    | ELSE cuerpoOSentencia     // else { ... }  /  else sentencia
    ;

// ── Switch (con fallthrough real: break es una sentencia normal) ─

seleccion
    : SWITCH PAREN_IZQ expresion PAREN_DER LLAVE_IZQ casoBloque* LLAVE_DER
    ;

casoBloque
    : CASE expresion DOS_PUNTOS sentencia*
    | DEFAULT DOS_PUNTOS sentencia*
    ;

// ── Ciclos ───────────────────────────────────────────────────

forClasico
    : FOR PAREN_IZQ forInit? PUNTO_COMA expresion? PUNTO_COMA forUpdate? PAREN_DER cuerpoOSentencia
    ;

forInit
    : tipo arrayDims? IDENTIFICADOR ASIGNACION expresion   // for (int i = 0; ...)
    | accesoVariable ASIGNACION expresion                  // for (i = 0; ...)
    ;

forUpdate
    : accesoVariable (INCREMENTO | DECREMENTO)
    | accesoVariable ASIGNACION expresion
    ;

whileClasico
    : WHILE PAREN_IZQ expresion PAREN_DER cuerpoOSentencia
    ;

doWhileClasico
    : DO cuerpoOSentencia WHILE PAREN_IZQ expresion PAREN_DER PUNTO_COMA
    ;

// ── Expresiones (ternario incluido, precedencia estilo Java) ─

expresion
    : expresionOr (INTERROGACION expresion DOS_PUNTOS expresion)?
    ;

expresionOr
    : expresionAnd (OR expresionAnd)*
    ;

expresionAnd
    : expresionIgualdad (AND expresionIgualdad)*
    ;

expresionIgualdad
    : expresionRelacional ((IGUAL | DISTINTO) expresionRelacional)*
    ;

expresionRelacional
    : expresionAditiva ((MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL) expresionAditiva)*
    ;

expresionAditiva
    : expresionMultiplicativa ((MAS | MENOS) expresionMultiplicativa)*
    ;

expresionMultiplicativa
    : expresionUnaria ((POR | DIV | MODULO) expresionUnaria)*
    ;

expresionUnaria
    : NEGACION expresionUnaria
    | MENOS expresionUnaria
    | factor
    ;

// ── Accesos encadenados: variable, atributo, índice, llamada ─

accesoVariable
    : baseAcceso sufijoAcceso*
    ;

baseAcceso
    : IDENTIFICADOR
    | THIS
    ;

sufijoAcceso
    : PUNTO IDENTIFICADOR
    | CORCHETE_IZQ expresion CORCHETE_DER
    | PAREN_IZQ argumentos? PAREN_DER
    ;

argumentos
    : expresion (COMA expresion)*
    ;

factor
    : NUMERO
    | DECIMAL
    | CADENA
    | CARACTER
    | TRUE
    | FALSE
    | NULL
    | NEW IDENTIFICADOR (PAREN_IZQ argumentos? PAREN_DER | (CORCHETE_IZQ expresion CORCHETE_DER)+)  // new Persona(...) u objeto[]
    | NEW tipoBase (CORCHETE_IZQ expresion CORCHETE_DER)+                                           // new int[5], new int[3][3]
    | accesoVariable INCREMENTO                                                                     // postfijo: k++
    | accesoVariable DECREMENTO                                                                     // postfijo: k--
    | accesoVariable
    | literalArreglo
    | PAREN_IZQ expresion PAREN_DER
    ;

literalArreglo
    : LLAVE_IZQ expresion (COMA expresion)* LLAVE_DER
    ;

// ══════════════════════════════════════════════════════════════
//  LEXER / TERMINALES
// ══════════════════════════════════════════════════════════════

// ── Palabras reservadas ──────────────────────────────────────

CLASS     : 'class';
PUBLIC    : 'public';
PRIVATE   : 'private';
PROTECTED : 'protected';
EXTENDS   : 'extends';
THIS      : 'this';
SUPER     : 'super';
OVERRIDE  : '@Override';
VOID     : 'void';
NEW      : 'new';
NULL     : 'null';
TRUE     : 'true';
FALSE    : 'false';
IF       : 'if';
ELSE     : 'else';
SWITCH   : 'switch';
CASE     : 'case';
DEFAULT  : 'default';
BREAK    : 'break';
CONTINUE : 'continue';
FOR      : 'for';
WHILE    : 'while';
DO       : 'do';
RETURN   : 'return';
INT      : 'int';
DOUBLE   : 'double';
CHAR     : 'char';
BOOLEAN  : 'boolean';
STRING   : 'String';

// ── Operadores (los de dos/tres caracteres primero) ──────────

OR           : '||';
AND          : '&&';
IGUAL        : '==';
DISTINTO     : '!=';
MENOR_IGUAL  : '<=';
MAYOR_IGUAL  : '>=';
INCREMENTO   : '++';
DECREMENTO   : '--';
MAS_ASIGNA   : '+=';
MENOS_ASIGNA : '-=';
POR_ASIGNA   : '*=';
NEGACION     : '!';
MENOR        : '<';
MAYOR        : '>';
ASIGNACION   : '=';
MAS          : '+';
MENOS        : '-';
POR          : '*';
DIV          : '/';
MODULO       : '%';
INTERROGACION: '?';

// ── Símbolos ─────────────────────────────────────────────────

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
WS                : [ \t\r\n]+ -> channel(HIDDEN);
