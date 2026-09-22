// Ejemplo Y?: cubre todo el lenguaje
// estructuras, funciones con/sin retorno, parametros por referencia,
// condicionales, elegir, los 3 ciclos, romper/continuar, arreglos, E/S
%estructuras
estructura Persona:
    entero edad
    cadena nombre
    flotante promedio
    caracter inicial
    bool activo
    entero notas[3]
    Persona mejorAmigo
estructura Punto:
    entero x
    entero y
%funciones
definir sumar(entero a, entero b) -> entero:
    entero resultado = a + b
    retornar resultado
definir esMayor(entero edad) -> bool:
    si (edad > 17) entonces
        retornar verdadero
    contrario
        retornar falso
definir signo(entero n) -> entero:
    si (n > 0) entonces
        retornar 1
    sino (n == 0) entonces
        retornar 0
    contrario
        retornar -1
definir enRango(entero n) -> bool:
    si ((n > 0) && (n < 100)) entonces
        retornar verdadero
    sino ((n == 0) || (n == 100)) entonces
        retornar falso
    contrario
        retornar falso
definir procesar([] entero datos, {} Persona p) -> entero:
    datos[0] = datos[0] + 1
    p.edad = p.edad + 1
    retornar datos[0] + p.edad
definir mostrarInfo(Persona p):
    imprimir("nombre: " + p.nombre)
    imprimir(p.edad)
    imprimir(p.activo)
definir crearPersona() -> Persona:
    Persona p
    p.edad = 30
    p.nombre = "beto"
    p.promedio = 8.5
    p.inicial = 'B'
    p.activo = falso
    p.notas = {7, 8, 9}
    retornar p
definir origen() -> Punto:
    Punto o
    o.x = 0
    o.y = 0
    retornar o
definir datos():
    entero primos[3] = {2, 3, 5}
    entero m[2][3]
    m[0][0] = 1
    m[1][2] = primos[2]
    imprimir(m[0][0] + m[1][2])
definir clasificar(entero n):
    elegir (n):
        caso 1:
            imprimir("uno")
            romper
        caso 2:
            imprimir("dos")
            romper
        siempre:
            imprimir("otro")
            romper
definir factorial(entero n) -> entero:
    si (n == 0) entonces
        retornar 1
    contrario
        retornar n * factorial(n - 1)
definir ejemploCiclos():
    entero total = 0
    entero arr[5]
    entero division = 10 / 3
    bool bandera = verdadero
    para (entero i = 0; i < 5; i++):
        arr[i] = i * 2
        total = total + arr[i]
    mientras (total < 100) hacer
        total = total + 10
        si (total == 50) entonces
            continuar
        contrario
            total = total + 1
    hacer:
        total = total - 1
    mientras (total > 95)
    si (!bandera) entonces
        total = 0
    contrario
        total = total + 0
    entero n = leer()
    total = total + n
    n++
    n--
    imprimir("total: ")
    imprimir(total + division)
