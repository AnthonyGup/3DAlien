parser grammar YLangParser;

options { tokenVocab = YLangLexer; }

// Y? define bloques por indentación. Los tokens sintéticos INDENT/DEDENT
// los produce el lexer personalizado (ver YLangLexer.g4).

programa
    : NEWLINE* seccionEstructuras? seccionFunciones EOF
    ;

// ── Estructuras (sección opcional) ───────────────────────────

seccionEstructuras
    : ESTRUCTURAS_MARKER NEWLINE estructura+
    ;

estructura
    : ESTRUCTURA IDENTIFICADOR DOS_PUNTOS NEWLINE INDENT campoEstructura+ DEDENT
    ;

campoEstructura
    : tipoCampo IDENTIFICADOR (CORCHETE_IZQ NUMERO CORCHETE_DER)* NEWLINE?
    ;

tipoCampo
    : tipoPrimitivo
    | IDENTIFICADOR
    ;

// ── Funciones (sección obligatoria) ──────────────────────────

seccionFunciones
    : FUNCIONES_MARKER NEWLINE funcion+
    ;

funcion
    : DEFINIR IDENTIFICADOR PAREN_IZQ parametros? PAREN_DER (FLECHA tipo)? DOS_PUNTOS NEWLINE
      INDENT sentencia+ DEDENT
    ;

parametros
    : parametro (COMA parametro)*
    ;

parametro
    : (CORCHETES_VACIOS | LLAVES_VACIAS)? tipo IDENTIFICADOR
    ;

tipo
    : tipoPrimitivo
    | IDENTIFICADOR
    ;

tipoPrimitivo
    : ENTERO
    | FLOTANTE
    | CADENA_TIPO
    | CARACTER_TIPO
    | BOOL
    ;

// ── Sentencias ───────────────────────────────────────────────

sentencia
    : declaracionVariable
    | asignacion
    | incremento
    | decremento
    | accesoVariable NEWLINE?
    | condicional
    | seleccion
    | paraCiclo
    | mientrasCiclo
    | hacerMientrasCiclo
    | retornoSentencia
    | ROMPER NEWLINE?
    | CONTINUAR NEWLINE?
    ;

declaracionVariable
    : tipo IDENTIFICADOR (CORCHETE_IZQ NUMERO CORCHETE_DER)* (ASIGNACION expresion)? NEWLINE?
    ;

asignacion
    : accesoVariable ASIGNACION expresion NEWLINE?
    ;

incremento
    : accesoVariable INCREMENTO NEWLINE?
    ;

decremento
    : accesoVariable DECREMENTO NEWLINE?
    ;

retornoSentencia
    : RETORNAR expresion NEWLINE?
    ;

// ── Condicionales ────────────────────────────────────────────

condicional
    : SI PAREN_IZQ expresion PAREN_DER ENTONCES NEWLINE INDENT sentencia+ DEDENT
      ramaSino*
      ramaContrario?
    ;

ramaSino
    : SINO PAREN_IZQ expresion PAREN_DER ENTONCES NEWLINE INDENT sentencia+ DEDENT
    ;

ramaContrario
    : CONTRARIO NEWLINE INDENT sentencia+ DEDENT
    ;

// ── Switch ───────────────────────────────────────────────────

seleccion
    : ELEGIR PAREN_IZQ expresion PAREN_DER DOS_PUNTOS NEWLINE INDENT casoBloque+ DEDENT
    ;

casoBloque
    : CASO NUMERO DOS_PUNTOS NEWLINE INDENT sentencia+ DEDENT
    | SIEMPRE DOS_PUNTOS NEWLINE INDENT sentencia+ DEDENT
    ;

// ── Ciclos ───────────────────────────────────────────────────

paraCiclo
    : PARA PAREN_IZQ inicializadorPara PUNTO_COMA expresion PUNTO_COMA pasoPara PAREN_DER DOS_PUNTOS NEWLINE
      INDENT sentencia+ DEDENT
    ;

inicializadorPara
    : tipoPrimitivo IDENTIFICADOR ASIGNACION expresion
    ;

pasoPara
    : IDENTIFICADOR (INCREMENTO | DECREMENTO)
    ;

mientrasCiclo
    : MIENTRAS PAREN_IZQ expresion PAREN_DER HACER NEWLINE INDENT sentencia+ DEDENT
    ;

hacerMientrasCiclo
    : HACER DOS_PUNTOS NEWLINE INDENT sentencia+ DEDENT
      MIENTRAS PAREN_IZQ expresion PAREN_DER NEWLINE?
    ;

// ── Expresiones (precedencia: la más baja primero) ───────────

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
    : expresionAditiva ((MENOR | MAYOR) expresionAditiva)*
    ;

expresionAditiva
    : expresionMultiplicativa ((MAS | MENOS) expresionMultiplicativa)*
    ;

expresionMultiplicativa
    : expresionUnaria ((POR | DIV) expresionUnaria)*
    ;

expresionUnaria
    : NEGACION expresionUnaria
    | MENOS expresionUnaria
    | factor
    ;

// ── Accesos encadenados: variable, atributo, índice, llamada ─

accesoVariable
    : IDENTIFICADOR sufijoAcceso*
    ;

sufijoAcceso
    : PUNTO IDENTIFICADOR
    | CORCHETE_IZQ expresion CORCHETE_DER
    | PAREN_IZQ argumentos? PAREN_DER
    ;

argumentos
    : expresion (COMA expresion)*
    ;

literal
    : LLAVE_IZQ expresion (COMA expresion)* LLAVE_DER
    ;

factor
    : NUMERO
    | DECIMAL
    | CADENA
    | CARACTER
    | VERDADERO
    | FALSO
    | accesoVariable
    | literal
    | PAREN_IZQ expresion PAREN_DER
    ;