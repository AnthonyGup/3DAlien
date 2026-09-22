// Ejemplo Pig Latin: cubre todo el lenguaje
## variables, arreglos, aritmetica, comparaciones, logica,
condicionales, los 3 ciclos, perge/interrumpe, ++/--, lectura/escritura ##

VARIABILES>
    esto contador : numerus 0;
    esto total : numerus 0;
    esto promedio : decimalis 0.0;
    esto pi : decimalis 3.14;
    esto nombre : textum "3DAlien";
    esto saludo : textum "hola";
    esto inicial : littera 'A';
    esto activo : verum;
    esto listo : falsus;
    esto i : numerus 0;
    esto j : numerus 0;
    esto n : numerus 0;
    series numeros[5] : numerus {1, 2, 3, 4, 5};
    series notas[3] : decimalis {9.5, 8.0, 7.25};
    series nombres[2] : textum {"ana", "bob"};
MAIOR>
    >> "Inicio: " >> nombre;
    saludo = saludo + " mundo";
    >> saludo;
    contador = 10 + 5 * 2 - 20 / 4;
    promedio = (contador + 5) / 2;
    pi = pi * 2.0;
    n = -contador;
    si (contador >= 10 && activo) {
        >> "contador grande y activo";
    } aliter (contador <= 5 || listo) {
        >> "contador chico o listo";
    } aliter {
        >> "intermedio";
    } finis;
    si (nombre == "3DAlien") {
        >> "nombre correcto";
    } finis;
    si (non listo) {
        listo = verum;
    } finis;
    si (inicial == 'A') {
        >> inicial;
    } finis;
    si (contador != 0) {
        >> "distinto de cero";
    } finis;
    dum (i < 5) {
        total = total + numeros[i];
        i = i + 1;
    } finis;
    i = 0;
    dum (i < 3) {
        j = 0;
        dum (j < 2) {
            total = total + 1;
            j = j + 1;
        } finis;
        i = i + 1;
    } finis;
    j = 0;
    facere {
        j = j + 1;
        si (j == 2) {
            perge;
        } finis;
        total = total + j;
    } dum (j < 5);
    per (esto k : numerus 0; k < 3; k++) {
        n = n + k;
        si (k == 1) {
            interrumpe;
        } finis;
    } finis;
    contador++;
    contador--;
    numeros[0] = 100;
    notas[2] = 10.0;
    nombres[1] = "beto";
    >> numeros[0] >> notas[2] >> nombres[1];
    >> "Escribe un numero:";
    n <<;
    >> "Leiste: " >> n;
    >> "Fin. Total = " >> total;
FINIS;
