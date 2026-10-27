package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Punto unico para consultar el layout de clases y structs desde el backend RISC-V.
 * Los offsets se expresan en slots de 8 bytes.
 */
public final class LayoutsRiscV {
    private final ClaseLayouts clases;
    private final Map<String, Symbol> tipos = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> camposStruct = new LinkedHashMap<>();

    public LayoutsRiscV(SymbolTable tabla, ClaseLayouts clases) {
        this.clases = clases;
        if (tabla != null) {
            for (Symbol s : tabla.listarGlobales()) {
                if (s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA) {
                    tipos.put(s.getName(), s);
                }
            }
        }
    }

    public boolean esClase(String tipo) {
        Symbol s = tipos.get(tipo);
        return s != null && s.getKind() == Symbol.Kind.CLASE;
    }

    public int slots(String tipo) {
        if (tipo == null) {
            return 1;
        }
        if (esClase(tipo) && clases != null && clases.contiene(tipo)) {
            return Math.max(1, clases.tamanoDe(tipo));
        }
        return Math.max(1, camposDe(tipo).size());
    }

    public int offsetCampo(String tipo, String campo) {
        if (tipo == null || campo == null) {
            return -1;
        }
        if (esClase(tipo) && clases != null && clases.contiene(tipo)) {
            return clases.offsetCampo(tipo, campo);
        }
        Integer offset = camposDe(tipo).get(campo);
        return offset == null ? -1 : offset;
    }

    public int idClase(String tipo) {
        if (esClase(tipo) && clases != null && clases.contiene(tipo)) {
            return clases.idDe(tipo);
        }
        return -1;
    }

    public TipoRV tipoCampo(String tipo, int idx) {
        Symbol s = tipos.get(tipo);
        if (s != null && s.getMiembros() != null) {
            int i = 0;
            for (Symbol f : s.getMiembros().getTodos()) {
                if (f.getKind() == Symbol.Kind.VARIABLE) {
                    if (i == idx) {
                        return TipoRV.desde(f.getType());
                    }
                    i++;
                }
            }
        }
        return TipoRV.ENTERO;
    }

    private Map<String, Integer> camposDe(String tipo) {
        return camposStruct.computeIfAbsent(tipo, k -> {
            Map<String, Integer> campos = new LinkedHashMap<>();
            Symbol s = tipos.get(k);
            int i = 0;
            if (s != null && s.getMiembros() != null) {
                for (Symbol f : s.getMiembros().getTodos()) {
                    if (f.getKind() == Symbol.Kind.VARIABLE) {
                        campos.put(f.getName(), i++);
                    }
                }
            }
            return campos;
        });
    }
}
