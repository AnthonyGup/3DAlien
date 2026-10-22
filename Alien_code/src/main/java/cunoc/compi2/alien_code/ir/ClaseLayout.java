package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.decl.Visibilidad;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout de una clase calculado antes de generar cuartetas (Fase 3).
 * No genera codigo: solo decide id de clase, offsets de campos y slots de vtable.
 */
public class ClaseLayout {
    public static class ImplMetodo {
        public final String nombre;
        public final String claseImpl;
        public final String etiqueta;
        public int claseId;

        public ImplMetodo(String nombre, String claseImpl, String etiqueta) {
            this.nombre = nombre;
            this.claseImpl = claseImpl;
            this.etiqueta = etiqueta;
        }
    }

    public final String nombre;
    public final int id;
    public ClaseLayout padre;
    public int tamano;
    public final Map<String, Integer> offsetCampo = new LinkedHashMap<>();
    public final List<ImplMetodo> vtable = new ArrayList<>();
    public final Map<String, Integer> slotDe = new LinkedHashMap<>();

    public ClaseLayout(String nombre, int id) {
        this.nombre = nombre;
        this.id = id;
        this.tamano = 1; // slot 0 = cabecera (id de clase)
    }

    public int offsetCampo(String campo) {
        Integer o = offsetCampo.get(campo);
        return o == null ? -1 : o;
    }

    public boolean tieneSlot(String metodo) {
        return slotDe.containsKey(metodo);
    }

    public int slotDe(String metodo) {
        Integer s = slotDe.get(metodo);
        return s == null ? -1 : s;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Clase ").append(nombre).append(" id=").append(id)
            .append(" tamano=").append(tamano);
        if (padre != null) sb.append(" extends ").append(padre.nombre);
        sb.append("\n  campos: ").append(offsetCampo);
        sb.append("\n  vtable:");
        for (int i = 0; i < vtable.size(); i++) {
            ImplMetodo m = vtable.get(i);
            sb.append("\n    [").append(i).append("] ").append(m.nombre)
              .append(" -> ").append(m.etiqueta);
        }
        return sb.toString();
    }
}
