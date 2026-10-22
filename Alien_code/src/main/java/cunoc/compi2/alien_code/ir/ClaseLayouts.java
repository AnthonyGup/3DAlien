package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.decl.Visibilidad;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construye y consulta los layouts de todas las clases (Fase 3).
 * Se instancia una vez, despues del semantico, antes de generar cuartetas.
 */
public class ClaseLayouts {
    private final Map<String, ClaseLayout> porNombre = new LinkedHashMap<>();
    private final Map<String, Symbol> simboloPorClase = new LinkedHashMap<>();

    public ClaseLayouts(SymbolTable tabla) {
        if (tabla == null) {
            return;
        }
        List<Symbol> clases = new ArrayList<>();
        for (Symbol s : tabla.listarGlobales()) {
            if (s.getKind() == Symbol.Kind.CLASE) {
                clases.add(s);
            }
        }
        for (Symbol c : clases) {
            simboloPorClase.put(c.getName(), c);
        }
        // Padres antes que hijos: orden topologico simple.
        Set<String> hechas = new LinkedHashSet<>();
        for (Symbol c : clases) {
            construir(c, hechas);
        }
        verificarInvariante();
    }

    private ClaseLayout construir(Symbol clase, Set<String> hechas) {
        String nombre = clase.getName();
        ClaseLayout existente = porNombre.get(nombre);
        if (existente != null) {
            return existente;
        }
        Symbol superSimbolo = clase.getSuperclaseResuelta();
        ClaseLayout padre = null;
        if (superSimbolo != null) {
            padre = construir(superSimbolo, hechas);
        }

        ClaseLayout layout = new ClaseLayout(nombre, porNombre.size() + 1);
        layout.padre = padre;
        if (padre != null) {
            layout.tamano = padre.tamano;
            layout.offsetCampo.putAll(padre.offsetCampo);
            layout.vtable.addAll(padre.vtable);
            layout.slotDe.putAll(padre.slotDe);
        }
        porNombre.put(nombre, layout);

        // Campos propios, en orden de declaracion.
        if (clase.getMiembros() != null) {
            for (Symbol f : clase.getMiembros().getTodos()) {
                if (f.getKind() == Symbol.Kind.VARIABLE && !f.isParameter()) {
                    if (!layout.offsetCampo.containsKey(f.getName())) {
                        layout.offsetCampo.put(f.getName(), layout.tamano);
                        layout.tamano++;
                    }
                }
            }
            // Metodos propios no privados: vtable.
            for (Symbol m : clase.getMiembros().getTodos()) {
                if (m.getKind() != Symbol.Kind.METODO) {
                    continue;
                }
                if (m.getVisibilidad() == Visibilidad.PRIVATE) {
                    continue;
                }
                int idx = 0;
                String etiqueta = NombresInternos.etiquetaMetodo(nombre, m.getName(), idx);
                ClaseLayout.ImplMetodo impl = new ClaseLayout.ImplMetodo(m.getName(), nombre, etiqueta);
                Integer slot = layout.slotDe.get(m.getName());
                if (slot != null) {
                    layout.vtable.set(slot, impl);
                } else {
                    layout.slotDe.put(m.getName(), layout.vtable.size());
                    layout.vtable.add(impl);
                }
            }
        }
        return layout;
    }

    private void verificarInvariante() {
        for (ClaseLayout layout : porNombre.values()) {
            if (layout.padre == null) {
                continue;
            }
            ClaseLayout p = layout.padre;
            for (Map.Entry<String, Integer> e : p.offsetCampo.entrySet()) {
                Integer propio = layout.offsetCampo.get(e.getKey());
                if (propio == null || !propio.equals(e.getValue())) {
                    throw new IllegalStateException(
                        "Invariante de layout rota: campo '" + e.getKey() + "' de " + p.nombre
                        + " tiene offset distinto en " + layout.nombre);
                }
            }
            for (int s = 0; s < p.vtable.size(); s++) {
                if (!layout.vtable.get(s).nombre.equals(p.vtable.get(s).nombre)) {
                    throw new IllegalStateException(
                        "Invariante de vtable rota en slot " + s + " de " + layout.nombre);
                }
            }
        }
    }

    public ClaseLayout layoutDe(String clase) {
        return porNombre.get(clase);
    }

    public Symbol simboloDe(String clase) {
        return simboloPorClase.get(clase);
    }

    public int offsetCampo(String claseEstatica, String campo) {
        ClaseLayout l = porNombre.get(claseEstatica);
        return l == null ? -1 : l.offsetCampo(campo);
    }

    public int tamanoDe(String clase) {
        ClaseLayout l = porNombre.get(clase);
        return l == null ? 0 : l.tamano;
    }

    public int idDe(String clase) {
        ClaseLayout l = porNombre.get(clase);
        return l == null ? 0 : l.id;
    }

    public boolean contiene(String clase) {
        return porNombre.containsKey(clase);
    }

    public List<ClaseLayout> descendientesDe(String clase) {
        List<ClaseLayout> res = new ArrayList<>();
        for (ClaseLayout l : porNombre.values()) {
            if (esDescendienteOIgual(l, clase)) {
                res.add(l);
            }
        }
        return res;
    }

    private boolean esDescendienteOIgual(ClaseLayout l, String clase) {
        ClaseLayout actual = l;
        int guarda = 0;
        while (actual != null && guarda++ < 1000) {
            if (actual.nombre.equals(clase)) {
                return true;
            }
            actual = actual.padre;
        }
        return false;
    }

    /**
     * Despacho de 'metodo' para el subarbol de 'claseEstatica': una entrada por
     * clase concreta descendiente (o igual), con su id y la etiqueta a invocar.
     */
    public List<ClaseLayout.ImplMetodo> candidatos(String claseEstatica, String metodo) {
        List<ClaseLayout.ImplMetodo> res = new ArrayList<>();
        for (ClaseLayout l : descendientesDe(claseEstatica)) {
            Integer slot = l.slotDe.get(metodo);
            if (slot == null || slot >= l.vtable.size()) {
                continue;
            }
            ClaseLayout.ImplMetodo impl = l.vtable.get(slot);
            ClaseLayout.ImplMetodo entrada =
                new ClaseLayout.ImplMetodo(impl.nombre, impl.claseImpl, impl.etiqueta);
            entrada.claseId = l.id;
            res.add(entrada);
        }
        return res;
    }

    public Map<String, ClaseLayout> todos() {
        return porNombre;
    }
}
