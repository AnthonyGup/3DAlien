package cunoc.compi2.alien_code.codegen.riscv;

import java.util.ArrayList;
import java.util.List;

public final class ConvencionLlamada {
    private ConvencionLlamada() {}

    public enum Donde { REG_ENTERO, REG_DOBLE, PILA }

    public record UbicacionArg(Donde donde, int indiceReg, int offsetPila) {}

    public static List<UbicacionArg> asignar(List<TipoRV> tipos) {
        int ints = 0, dbls = 0, pila = 0;
        List<UbicacionArg> r = new ArrayList<>();
        for (TipoRV t : tipos) {
            if (t == TipoRV.DOBLE) {
                if (dbls < 8) r.add(new UbicacionArg(Donde.REG_DOBLE, dbls++, 0));
                else { pila = (pila + 7) & ~7; r.add(new UbicacionArg(Donde.PILA, 0, pila)); pila += 8; }
            } else {
                if (ints < 8) r.add(new UbicacionArg(Donde.REG_ENTERO, ints++, 0));
                else { pila = (pila + 3) & ~3; r.add(new UbicacionArg(Donde.PILA, 0, pila)); pila += 4; }
            }
        }
        return r;
    }

    public static int salientes(List<UbicacionArg> u, List<TipoRV> t) {
        int maxOff = 0;
        for (int i = 0; i < u.size(); i++) {
            UbicacionArg ua = u.get(i);
            if (ua.donde() == Donde.PILA) {
                int tam = (t.get(i) == TipoRV.DOBLE) ? 8 : 4;
                maxOff = Math.max(maxOff, ua.offsetPila() + tam);
            }
        }
        return maxOff;
    }
}