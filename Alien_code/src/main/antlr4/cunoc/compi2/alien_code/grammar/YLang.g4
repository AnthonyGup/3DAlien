grammar YLang;

programa
    : declaracion* EOF
    ;

declaracion
    : functionDecl
    | varDecl
    | structDecl
    ;

functionDecl
    : tipo ID '(' parametros? ')' bloque
    ;

varDecl
    : tipo ID ('[' LITERAL_ENTERO ']')* ('=' expresion)? ';'
    ;

structDecl
    : 'estructura' ID '{' campo* '}'
    ;

campo
    : tipo ID ';'
    ;

tipo
    : 'entero'
    | 'decimal'
    | 'texto'
    | 'booleano'
    | 'caracter'
    | 'vacio'
    | ID
    ;

parametros
    : parametro (',' parametro)*
    ;

parametro
    : tipo ID
    ;

bloque
    : '{' sentencia* '}'
    ;

sentencia
    : bloque
    | varDecl
    | sentenciaIf
    | sentenciaWhile
    | sentenciaFor
    | sentenciaReturn
    | sentenciaPrint
    | sentenciaRead
    | expresion ';'
    | asignacion
    ;

sentenciaIf
    : 'si' '(' expresion ')' bloque ('aliter' '(' expresion ')' bloque)* ('sino' bloque)?
    ;

sentenciaWhile
    : 'mientras' '(' expresion ')' bloque
    ;

sentenciaFor
    : 'para' '(' (varDecl | asignacion | expresion) ';' expresion ';' expresion ')' bloque
    ;

sentenciaReturn
    : 'regresar' expresion? ';'
    ;

sentenciaPrint
    : 'imprimir' '(' expresion (',' expresion)* ')' ';'
    ;

sentenciaRead
    : 'leer' '(' ID ')' ';'
    ;

asignacion
    : ID '=' expresion ';'
    | ID '[' expresion ']' '=' expresion ';'
    ;

expresion
    : LITERAL_ENTERO                          # LiteralInt
    | LITERAL_DECIMAL                         # LiteralFloat
    | LITERAL_CADENA                          # LiteralString
    | LITERAL_CARACTER                        # LiteralChar
    | 'verdadero'                             # LiteralBool
    | 'falso'                                # LiteralBool
    | ID                                     # Identificador
    | ID '(' (expresion (',' expresion)*)? ')' # LlamadaFuncion
    | ID '[' expresion ']'                    # AccesoArreglo
    | expresion '.' ID                        # AccesoCampo
    | '(' expresion ')'                      # Parentesis
    | expresion operadorBinario expresion     # ExprBinaria
    | operadorUnario expresion                # ExprUnaria
    | expresion '?' expresion ':' expresion   # Ternario
    ;

operadorBinario
    : '+' | '-' | '*' | '/' | '%'
    | '==' | '!=' | '<' | '>' | '<=' | '>='
    | '&&' | '||'
    ;

operadorUnario
    : '-' | '!'
    ;

LITERAL_ENTERO   : [0-9]+ ;
LITERAL_DECIMAL  : [0-9]+ '.' [0-9]+ ;
LITERAL_CADENA   : '"' (~["\r\n])* '"' ;
LITERAL_CARACTER : '\'' (~['\r\n]) '\'' ;
ID               : [a-zA-Z_][a-zA-Z0-9_]* ;
WS               : [ \t\r\n]+ -> skip ;
COMENTARIO_LINEA : '//' ~[\r\n]* -> skip ;
COMENTARIO_BLOQUE: '/*' .*? '*/' -> skip ;
