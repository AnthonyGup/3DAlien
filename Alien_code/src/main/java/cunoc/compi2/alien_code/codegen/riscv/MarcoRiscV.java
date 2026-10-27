package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarcoRiscV {
    public static final class Param {
        public final String nombre;
        public final TipoRV tipo;
        public final int slot;

        public Param(String nombre, TipoRV tipo, int slot) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.slot = slot;
        }
    }

    public String nombre;
    public final List<Param> params = new ArrayList<>();
    public int maxSlot = -1;
    public int salientes = 0;

    public MarcoRiscV() {}

    public static MarcoRiscV desde(String nombre, String descripcion) {
        MarcoRiscV marco = new MarcoRiscV();
        marco.nombre = nombre;
        if (descripcion == null || descripcion.isBlank()) {
            return marco;
        }
        int slot = 3;
        for (String p : descripcion.split(";")) {
            String param = p.trim();
            String tipo = "";
            int idx = param.indexOf(':');
            if (idx > 0) {
                tipo = param.substring(idx + 1).trim();
                param = param.substring(0, idx).trim();
            }
            if (param.isEmpty()) {
                continue;
            }
            if ("self".equals(param)) {
                marco.agregarParam("self", TipoRV.REF, 2);
            } else {
                marco.agregarParam(param, TipoRV.desdeString(tipo), slot++);
            }
        }
        return marco;
    }

    public void agregarParam(String nombre, TipoRV tipo, int slot) {
        params.add(new Param(nombre, tipo, slot));
        if (slot > maxSlot) maxSlot = slot;
    }

    public int tamano() {
        int base = 16 + 8 * (maxSlot + 1) + salientes;
        return (base + 15) & ~15;
    }
}