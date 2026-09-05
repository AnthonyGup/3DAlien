grammar Zetariano;

programa
    : declaracion* EOF
    ;

declaracion
    : functionDecl
    | varDecl
    | classDecl
    ;

functionDecl
    : tipo ID '(' parametros? ')' bloque
    ;

varDecl
    : tipo ID ('[' LITERAL_ENTERO ']')* ('=' expresion)? ';'
    ;

classDecl
    : 'classis' ID ('heredis' ID)? '{' miembro* '}'
    ;

miembro
    : campo
    | functionDecl
    ;

campo
    : tipo ID ';'
    ;

tipo
    : 'numerus'
    | 'numerusdecimalis'
    | 'verbum'
    | 'verum'
    | 'littera'
    | 'vacuum'
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
    | sentenciaDoWhile
    | sentenciaReturn
    | sentenciaPrint
    | sentenciaRead
    | expresion ';'
    | asignacion
    ;

sentenciaIf
    : 'si' '(' expresion ')' bloque ('aliter' '(' expresion ')' bloque)* ('sinon' bloque)?
    ;

sentenciaWhile
    : 'dum' '(' expresion ')' bloque
    ;

sentenciaFor
    : 'pro' '(' (varDecl | asignacion | expresion) ';' expresion ';' expresion ')' bloque
    ;

sentenciaDoWhile
    : 'fac' bloque 'dum' '(' expresion ')' ';'
    ;

sentenciaReturn
    : 'redden' expresion? ';'
    ;

sentenciaPrint
    : 'scribe' '(' expresion (',' expresion)* ')' ';'
    ;

sentenciaRead
    : 'cape' '(' ID ')' ';'
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
    | 'verum'                                 # LiteralBool
    | 'falsum'                                # LiteralBool
    | ID                                     # Identificador
    | ID '(' (expresion (',' expresion)*)? ')' # LlamadaFuncion
    | ID '[' expresion ']'                    # AccesoArreglo
    | expresion '.' ID                        # AccesoCampo
    | expresion '.' ID '(' (expresion (',' expresion)*)? ')' # LlamadaMetodo
    | 'novo' ID '(' (expresion (',' expresion)*)? ')' # CrearObjeto
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
