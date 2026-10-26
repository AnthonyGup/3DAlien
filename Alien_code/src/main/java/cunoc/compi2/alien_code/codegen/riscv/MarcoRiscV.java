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

    public void agregarParam(String nombre, TipoRV tipo, int slot) {
        params.add(new Param(nombre, tipo, slot));
        if (slot > maxSlot) maxSlot = slot;
    }

    public int tamano() {
        int base = 16 + 8 * (maxSlot + 1) + salientes;
        return (base + 15) & ~15;
    }

    public int slotByte(int slot) {
        return -(12 + 8 * slot);
    }
}