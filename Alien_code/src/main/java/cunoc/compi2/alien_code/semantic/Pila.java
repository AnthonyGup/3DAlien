package cunoc.compi2.alien_code.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Pila LIFO propia (reemplaza Deque/LinkedList en el contexto semantico).
 * sacar()/tope() son tolerantes a pila vacia: devuelven null.
 */
public final class Pila<T> {

    private final List<T> elementos = new ArrayList<>();

    public void empujar(T elemento) {
        elementos.add(elemento);
    }

    public T sacar() {
        if (elementos.isEmpty()) {
            return null;
        }
        return elementos.remove(elementos.size() - 1);
    }

    public T tope() {
        if (elementos.isEmpty()) {
            return null;
        }
        return elementos.get(elementos.size() - 1);
    }

    public boolean estaVacia() {
        return elementos.isEmpty();
    }
}
