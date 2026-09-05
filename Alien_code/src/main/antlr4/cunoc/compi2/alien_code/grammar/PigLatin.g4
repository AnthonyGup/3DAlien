grammar PigLatin;

program
    : declaration* EOF
    ;

declaration
    : functionDecl
    | varDecl
    | classDecl
    ;

functionDecl
    : type_ ID '(' parameters? ')' block
    ;

varDecl
    : type_ ID ('[' LITERAL_INT ']')* ('=' expression)? ';'
    ;

classDecl
    : 'class' ID ('extends' ID)? '{' member* '}'
    ;

member
    : field
    | functionDecl
    ;

field
    : type_ ID ';'
    ;

type_
    : 'int'
    | 'float'
    | 'string'
    | 'bool'
    | 'char'
    | 'void'
    | ID
    ;

parameters
    : parameter (',' parameter)*
    ;

parameter
    : type_ ID
    ;

block
    : '{' statement* '}'
    ;

statement
    : block
    | varDecl
    | ifStatement
    | whileStatement
    | forStatement
    | doWhileStatement
    | returnStatement
    | printStatement
    | readStatement
    | expression ';'
    | assignment
    ;

ifStatement
    : 'if' '(' expression ')' block ('elif' '(' expression ')' block)* ('else' block)?
    ;

whileStatement
    : 'while' '(' expression ')' block
    ;

forStatement
    : 'for' '(' (varDecl | assignment | expression) ';' expression ';' expression ')' block
    ;

doWhileStatement
    : 'do' block 'while' '(' expression ')' ';'
    ;

returnStatement
    : 'return' expression? ';'
    ;

printStatement
    : 'print' '(' expression (',' expression)* ')' ';'
    ;

readStatement
    : 'read' '(' ID ')' ';'
    ;

assignment
    : ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    ;

expression
    : LITERAL_INT                             # LiteralInt
    | LITERAL_FLOAT                           # LiteralFloat
    | LITERAL_STRING                          # LiteralString
    | LITERAL_CHAR                            # LiteralChar
    | 'true'                                  # LiteralBool
    | 'false'                                 # LiteralBool
    | ID                                      # Identifier
    | ID '(' (expression (',' expression)*)? ')' # FunctionCall
    | ID '[' expression ']'                   # ArrayAccess
    | expression '.' ID                       # FieldAccess
    | expression '.' ID '(' (expression (',' expression)*)? ')' # MethodCall
    | 'new' ID '(' (expression (',' expression)*)? ')' # ObjectCreation
    | '(' expression ')'                      # Parentheses
    | expression operatorBin expression       # BinaryExpr
    | operatorUnary expression                # UnaryExpr
    | expression '?' expression ':' expression # TernaryExpr
    ;

operatorBin
    : '+' | '-' | '*' | '/' | '%'
    | '==' | '!=' | '<' | '>' | '<=' | '>='
    | '&&' | '||'
    ;

operatorUnary
    : '-' | '!'
    ;

LITERAL_INT    : [0-9]+ ;
LITERAL_FLOAT  : [0-9]+ '.' [0-9]+ ;
LITERAL_STRING : '"' (~["\r\n])* '"' ;
LITERAL_CHAR   : '\'' (~['\r\n]) '\'' ;
ID             : [a-zA-Z_][a-zA-Z0-9_]* ;
WS             : [ \t\r\n]+ -> skip ;
LINE_COMMENT   : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT  : '/*' .*? '*/' -> skip ;
