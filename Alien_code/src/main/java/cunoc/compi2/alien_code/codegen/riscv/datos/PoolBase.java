package cunoc.compi2.alien_code.codegen.riscv.datos;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PoolBase {
    private final Map<String, Integer> indices = new LinkedHashMap<>();
    protected int siguiente;

    public int registrar(String texto) {
        Integer existente = indices.get(texto);
        if (existente != null) {
            return existente;
        }
        int indice = siguiente;
        indices.put(texto, indice);
        alRegistrar(texto, indice);
        return indice;
    }

    public Integer indice(String texto) {
        return indices.get(texto);
    }

    public Map<String, Integer> entradas() {
        return indices;
    }

    public int tamano() {
        return siguiente;
    }

    protected abstract void alRegistrar(String texto, int indice);
}
