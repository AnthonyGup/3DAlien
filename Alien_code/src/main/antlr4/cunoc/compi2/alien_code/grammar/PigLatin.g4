grammar PigLatin;

programa
    : importacion* seccionVariables? seccionPrincipal EOF
    ;

importacion
    : IMPORT rutaArchivo PUNTO_COMA?
    ;

rutaArchivo
    : IDENTIFICADOR (PUNTO IDENTIFICADOR)*
    ;

seccionVariables
    : VARIABILES_MARKER declaracion*
    ;

seccionPrincipal
    : MAIOR_MARKER sentenciaPrincipal* PROGRAMA_FIN PUNTO_COMA
    ;

sentenciaPrincipal
    : sentencia
    | condicional
    ;

declaracion
    : declaracionVariable
    | declaracionArreglo
    ;

declaracionVariable
    : ESTO IDENTIFICADOR DOS_PUNTOS (VERUM | FALSUS) PUNTO_COMA
    | ESTO IDENTIFICADOR DOS_PUNTOS tipoPrimitivo expresion PUNTO_COMA
    | ESTO IDENTIFICADOR DOS_PUNTOS NOVUS IDENTIFICADOR PAREN_IZQ argumentos? PAREN_DER PUNTO_COMA
    | ESTO IDENTIFICADOR DOS_PUNTOS IDENTIFICADOR literalStructura PUNTO_COMA
    | ESTO IDENTIFICADOR DOS_PUNTOS IDENTIFICADOR PUNTO_COMA
    ;

declaracionArreglo
    : SERIES IDENTIFICADOR CORCHETE_IZQ NUMERO CORCHETE_DER DOS_PUNTOS tipo inicializadorArreglo? PUNTO_COMA
    ;

literalStructura
    : LLAVE_IZQ valorAtributo (COMA valorAtributo)* LLAVE_DER
    ;

valorAtributo
    : literalStructura
    | expresion
    ;

inicializadorArreglo
    : LLAVE_IZQ expresion (COMA expresion)* LLAVE_DER
    ;

tipo
    : tipoPrimitivo
    | IDENTIFICADOR
    ;

tipoPrimitivo
    : NUMERUS
    | DECIMALIS
    | TEXTUM
    | LITTERA
    | BOOL
    ;

argumentos
    : expresion (COMA expresion)*
    ;

sentencia
    : asignacion
    | incremento
    | decremento
    | ciclo
    | perge
    | interrumpe
    | accesoVariable PUNTO_COMA
    | leer
    | imprimir
    ;

leer
    : LEER PUNTO_COMA?
    | accesoVariable LEER PUNTO_COMA?
    ;

imprimir
    : ESCRIBIR (ESCRIBIR? expresion)+ PUNTO_COMA
    ;

ciclo
    : dum
    | facere
    | per
    ;

dum
    : DUM PAREN_IZQ expresion PAREN_DER cuerpoBloque FINIS PUNTO_COMA
    ;

facere
    : FACERE cuerpoBloque DUM PAREN_IZQ expresion PAREN_DER PUNTO_COMA
    ;

per
    : PER PAREN_IZQ inicializadorCiclo PUNTO_COMA expresion PUNTO_COMA pasoCiclo PAREN_DER cuerpoBloque (FINIS PUNTO_COMA)?
    ;

inicializadorCiclo
    : ESTO IDENTIFICADOR DOS_PUNTOS tipoPrimitivo expresion
    ;

pasoCiclo
    : IDENTIFICADOR INCREMENTO
    | IDENTIFICADOR DECREMENTO
    ;

perge
    : PERGE PUNTO_COMA
    ;

interrumpe
    : INTERRUMPE PUNTO_COMA
    ;

condicional
    : SI PAREN_IZQ expresion PAREN_DER cuerpoBloque aliter* FINIS PUNTO_COMA
    ;

aliter
    : ALITER (PAREN_IZQ expresion PAREN_DER)? cuerpoBloque
    ;

cuerpoBloque
    : LLAVE_IZQ sentenciaPrincipal* LLAVE_DER
    ;

asignacion
    : accesoVariable ASIGNACION expresion PUNTO_COMA
    ;

incremento
    : accesoVariable INCREMENTO PUNTO_COMA
    ;

decremento
    : accesoVariable DECREMENTO PUNTO_COMA
    ;

expresion
    : expresionOr
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
    : expresionUnaria ((POR | DIV) expresionUnaria)*
    ;

expresionUnaria
    : NON expresionUnaria
    | MENOS expresionUnaria
    | factor
    ;

accesoVariable
    : IDENTIFICADOR sufijoAcceso*
    ;

sufijoAcceso
    : PUNTO IDENTIFICADOR
    | CORCHETE_IZQ expresion CORCHETE_DER
    | PAREN_IZQ argumentos? PAREN_DER
    ;

factor
    : NUMERO
    | DECIMAL
    | CADENA
    | CARACTER
    | VERUM
    | FALSUS
    | NOVUS IDENTIFICADOR PAREN_IZQ argumentos? PAREN_DER
    | accesoVariable
    | literalStructura
    | PAREN_IZQ expresion PAREN_DER
    ;

VARIABILES_MARKER : 'VARIABILES>';
MAIOR_MARKER       : 'MAIOR>';

IMPORT      : 'import';
ESTO        : 'esto';
SERIES      : 'series';
NOVUS       : 'novus';
FINIS       : 'finis';
PROGRAMA_FIN : 'FINIS';
SI          : 'si';
ALITER      : 'aliter';
DUM         : 'dum';
FACERE      : 'facere';
PER         : 'per';
PERGE       : 'perge';
INTERRUMPE  : 'interrumpe';
NON         : 'non';
VERUM       : 'verum';
FALSUS      : 'falsus';
NUMERUS     : 'numerus';
DECIMALIS   : 'decimalis';
TEXTUM      : 'textum';
LITTERA     : 'littera';
BOOL        : 'bool';

OR            : '||';
AND           : '&&';
IGUAL         : '==';
DISTINTO      : '!=';
MENOR_IGUAL   : '<=';
MAYOR_IGUAL   : '>=';
INCREMENTO    : '++';
DECREMENTO    : '--';
LEER          : '<<';
ESCRIBIR      : '>>';
MENOR         : '<';
MAYOR         : '>';
ASIGNACION    : '=';
MAS           : '+';
MENOS         : '-';
POR           : '*';
DIV           : '/';

PAREN_IZQ     : '(';
PAREN_DER     : ')';
LLAVE_IZQ     : '{';
LLAVE_DER     : '}';
CORCHETE_IZQ  : '[';
CORCHETE_DER  : ']';
DOS_PUNTOS    : ':';
PUNTO_COMA    : ';';
PUNTO         : '.';
COMA          : ',';

NUMERO          : [0-9]+;
DECIMAL         : NUMERO '.' NUMERO;
CADENA          : '"' ~'"'* '"';
CARACTER        : '\'' ~'\'' '\'';
IDENTIFICADOR   : [a-zA-Z_][a-zA-Z0-9_]*;

COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN);
COMENTARIO_BLOQUE : '##' .*? '##' -> channel(HIDDEN);
WS                : [ \t\r\n]+ -> channel(HIDDEN);