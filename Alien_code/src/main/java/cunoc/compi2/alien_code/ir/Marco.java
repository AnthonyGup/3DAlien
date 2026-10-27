package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Marco {

    public final String nombre;
    public final boolean esFuncion;
    public boolean tieneSelf = false;
    public final Map<String, Integer> parametros = new LinkedHashMap<>();
    public final Map<Symbol, Integer> locales = new IdentityHashMap<>();
    public final Map<Integer, Integer> temporales = new HashMap<>();
    public int alto = 3;
    public int tamano = -1;

    public Marco(String nombre, boolean esFuncion) {
        this.nombre = nombre;
        this.esFuncion = esFuncion;
    }

}
