package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.errors.ErrorListener;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntermediateCodeGenerator implements CodigoContexto {
    private final List<Cuarteta> instrucciones;
    private final Map<Integer, String> tiposTemporales;
    private final Deque<String[]> ciclos;
    private final SymbolTable tabla;
    private int tempCounter;
    private int labelCounter;
    private final Deque<InfoClase> pilaClases = new ArrayDeque<>();
    private Map<String, Symbol> simbolosPlano;

    private final Set<Symbol> globales = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Symbol, Integer> offsetsGlobales = new IdentityHashMap<>();
    private final Map<String, Symbol> tiposGlobales = new HashMap<>();
    private final Map<String, Map<String, Integer>> camposPorTipo = new HashMap<>();
    private int siguienteGlobal = 0;
    private final Deque<Marco> marcos = new ArrayDeque<>();
    private final Map<String, Marco> marcosPorFuncion = new LinkedHashMap<>();
    private final Map<Llamada3D, Marco> llamadasPendientes = new IdentityHashMap<>();
    private final Map<cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D, Marco> llamadasVirtuales = new IdentityHashMap<>();
    private boolean layoutFinalizado = false;
    private int totalGlobales = -1;
    private final ClaseLayouts layouts;

    public static final class Marco {
        public final String nombre;
        public final boolean esFuncion;
        public boolean tieneSelf = false;
        public final Map<String, Integer> parametros = new LinkedHashMap<>();
        public final Map<Symbol, Integer> locales = new IdentityHashMap<>();
        public final Map<Integer, Integer> temporales = new HashMap<>();
        public int alto = 3;
        public int tamano = -1;

        Marco(String nombre, boolean esFuncion) {
            this.nombre = nombre;
            this.esFuncion = esFuncion;
        }
    }

    public IntermediateCodeGenerator(ErrorListener errorListener) {
        this(errorListener, null);
    }

    public IntermediateCodeGenerator(ErrorListener errorListener, SymbolTable tabla) {
        this.instrucciones = new ArrayList<>();
        this.tiposTemporales = new HashMap<>();
        this.ciclos = new ArrayDeque<>();
        this.tabla = tabla;
        this.tempCounter = 0;
        this.labelCounter = 0;
        this.layouts = new ClaseLayouts(tabla);
        inicializarGlobales();
    }

    public ClaseLayouts getLayouts() {
        return layouts;
    }

    public List<Cuarteta> getInstrucciones() {
        finalizarLayout();
        return instrucciones;
    }

    private void inicializarGlobales() {
        marcos.push(new Marco("<main>", false));
        if (tabla == null) {
            return;
        }
        for (Symbol s : tabla.listarGlobales()) {
            if (s.getKind() == Symbol.Kind.VARIABLE && !s.isField()) {
                globales.add(s);
            } else if (s.getKind() == Symbol.Kind.CLASE || s.getKind() == Symbol.Kind.ESTRUCTURA) {
                tiposGlobales.put(s.getName(), s);
            }
        }
    }

    public int offsetDe(Symbol s) {
        if (s == null) {
            return -1;
        }
        if (s.isField()) {
            return -1;
        }
        Marco m = marcos.peek();
        if (s.isParameter()) {
            Integer p = m.parametros.get(s.getName());
            if (p != null) {
                m.locales.put(s, p);
                return p;
            }
        }
        Integer local = m.locales.get(s);
        if (local != null) {
            return local;
        }
        if (globales.contains(s)) {
            Integer g = offsetsGlobales.get(s);
            if (g == null) {
                g = siguienteGlobal++;
                offsetsGlobales.put(s, g);
            }
            s.setOffset(g);
            s.setOffsetGlobal(true);
            return g;
        }
        int off = m.alto++;
        m.locales.put(s, off);
        s.setOffset(off);
        return off;
    }

    public int offsetTemporal(int numero) {
        Marco m = marcos.peek();
        Integer o = m.temporales.get(numero);
        if (o == null) {
            o = m.alto++;
            m.temporales.put(numero, o);
        }
        return o;
    }

    public int offsetCampo(String tipoNombre, String campo) {
        if (tipoNombre == null) {
            return -1;
        }
        if (layouts != null && layouts.contiene(tipoNombre)) {
            return layouts.offsetCampo(tipoNombre, campo);
        }
        Map<String, Integer> mapa = camposPorTipo.computeIfAbsent(tipoNombre, k -> {
            Map<String, Integer> mm = new LinkedHashMap<>();
            Symbol tipo = tiposGlobales.get(k);
            int i = 0;
            if (tipo != null && tipo.getMiembros() != null) {
                for (Symbol f : tipo.getMiembros().getTodos()) {
                    if (f.getKind() == Symbol.Kind.VARIABLE) {
                        mm.put(f.getName(), i++);
                    }
                }
            }
            return mm;
        });
        Integer o = mapa.get(campo);
        return o == null ? -1 : o;
    }

    public int cuentaCampos(String tipoNombre) {
        if (tipoNombre == null) {
            return 0;
        }
        offsetCampo(tipoNombre, "");
        Map<String, Integer> mapa = camposPorTipo.get(tipoNombre);
        return mapa == null ? 0 : mapa.size();
    }

    public int getTotalGlobales() {
        finalizarLayout();
        return totalGlobales;
    }

    public Marco getMarcoActual() {
        return marcos.peek();
    }

    private void finalizarLayout() {
        if (layoutFinalizado) {
            return;
        }
        layoutFinalizado = true;
        for (Marco m : marcos) {
            if (m.tamano < 0) {
                m.tamano = m.alto;
            }
        }
        for (Marco m : marcosPorFuncion.values()) {
            if (m.tamano < 0) {
                m.tamano = m.alto;
            }
        }
        for (Map.Entry<Llamada3D, Marco> e : llamadasPendientes.entrySet()) {
            Llamada3D lam = e.getKey();
            Marco m = e.getValue();
            boolean usuario = marcosPorFuncion.containsKey(lam.getFuncion());
            lam.setUsuario(usuario);
            if (usuario) {
                lam.setCallerFrameSize(m.tamano);
                Marco callee = marcosPorFuncion.get(lam.getFuncion());
                lam.setTieneSelf(callee != null && callee.tieneSelf);
            }
        }
        for (Map.Entry<cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D, Marco> e : llamadasVirtuales.entrySet()) {
            cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D virt = e.getKey();
            Marco m = e.getValue();
            virt.setCallerFrameSize(m.tamano);
            virt.setTieneSelf(true);
            virt.setUsuario(true);
        }
        totalGlobales = siguienteGlobal;
    }

    public Map<Integer, String> getTiposTemporales() {
        return tiposTemporales;
    }

    @Override
    public int nuevoIndiceTemporal() {
        return tempCounter++;
    }

    @Override
    public String nuevaEtiqueta() {
        return "L" + (labelCounter++);
    }

    @Override
    public void agregar(Cuarteta cuarteta) {
        if (cuarteta instanceof InicioFuncion3D ini) {
            Marco m = new Marco(ini.getNombre(), true);
            String desc = ini.getDescripcion();
            int slot = 3;
            if (desc != null && !desc.isBlank()) {
                for (String p : desc.split(";")) {
                    String nom = p.trim();
                    int idx = nom.indexOf(':');
                    if (idx > 0) {
                        nom = nom.substring(0, idx).trim();
                    }
                    if (nom.isEmpty()) {
                        continue;
                    }
                    if (nom.equals("self")) {
                        m.tieneSelf = true;
                        m.parametros.put("self", 2);
                    } else {
                        m.parametros.put(nom, slot++);
                    }
                }
            }
            m.alto = Math.max(3, slot);
            marcos.push(m);
            marcosPorFuncion.put(ini.getNombre(), m);
        }
        instrucciones.add(cuarteta);
        if (cuarteta instanceof Llamada3D lam) {
            llamadasPendientes.put(lam, marcos.peek());
        }
        if (cuarteta instanceof cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D virt) {
            llamadasVirtuales.put(virt, marcos.peek());
        }
        if (cuarteta instanceof cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D arr) {
            Symbol s = resolverSimbolo(arr.getNombre());
            if (s != null && s.getKind() == Symbol.Kind.VARIABLE) {
                offsetDe(s);
            }
        }
        if (cuarteta instanceof FinFuncion3D) {
            if (marcos.size() > 1) {
                Marco m = marcos.pop();
                m.tamano = m.alto;
            }
        }
    }

    @Override
    public void registrarTipoTemporal(int temporal, String ctype) {
        if (ctype != null && !ctype.equals("void")) {
            tiposTemporales.put(temporal, ctype);
        }
    }

    @Override
    public String tipoDeTemporal(int temporal) {
        return tiposTemporales.get(temporal);
    }

    @Override
    public Symbol resolverSimbolo(String nombre) {
        if (nombre == null) {
            return null;
        }
        if (simbolosPlano == null && tabla != null) {
            simbolosPlano = new HashMap<>();
            for (Symbol s : tabla.todosLosSimbolos()) {
                if (s.getKind() == Symbol.Kind.METODO || s.getKind() == Symbol.Kind.CONSTRUCTOR) {
                    continue;
                }
                simbolosPlano.putIfAbsent(s.getName(), s);
            }
        }
        return simbolosPlano == null ? null : simbolosPlano.get(nombre);
    }

    @Override
    public SymbolTable getSymbolTable() {
        return tabla;
    }

    @Override
    public void empujarCiclo(String etiquetaContinuar, String etiquetaSalida) {
        ciclos.push(new String[]{etiquetaContinuar, etiquetaSalida});
    }

    @Override
    public void popCiclo() {
        ciclos.pop();
    }

    @Override
    public String etiquetaContinuarActual() {
        return ciclos.isEmpty() ? null : ciclos.peek()[0];
    }

    @Override
    public String etiquetaSalidaActual() {
        return ciclos.isEmpty() ? null : ciclos.peek()[1];
    }

    @Override
    public void pushClassTranslation(String nombre) {
        pilaClases.push(new InfoClase(nombre));
    }

    public void setAtributosClase(java.util.List<cunoc.compi2.alien_code.ast.stmt.VariableDeclNode> atributos) {
        if (!pilaClases.isEmpty()) {
            pilaClases.peek().atributos = atributos;
        }
    }

    public java.util.List<cunoc.compi2.alien_code.ast.stmt.VariableDeclNode> getAtributosClase() {
        return pilaClases.isEmpty() ? java.util.Collections.emptyList() : pilaClases.peek().atributos;
    }

    @Override
    public void popClassTranslation() {
        if (!pilaClases.isEmpty()) {
            pilaClases.pop();
        }
    }

    @Override
    public String currentClassName() {
        return pilaClases.isEmpty() ? null : pilaClases.peek().nombre;
    }

    @Override
    public int nextMethodIndex(String methodName) {
        return pilaClases.isEmpty() ? 0 : pilaClases.peek().nextMethodIndex(methodName);
    }

    @Override
    public int nextConstructorIndex() {
        return pilaClases.isEmpty() ? 0 : pilaClases.peek().nextConstructorIndex();
    }

    public int overloadIndexByArgCount(List<List<Type>> firmas, int argCount) {
        for (int i = 0; i < firmas.size(); i++) {
            if (firmas.get(i).size() == argCount) {
                return i;
            }
        }
        return 0;
    }

    private static final class InfoClase {
        final String nombre;
        java.util.List<cunoc.compi2.alien_code.ast.stmt.VariableDeclNode> atributos =
            java.util.Collections.emptyList();
        final Map<String, Integer> indicesMetodos = new HashMap<>();
        int indicesConstructores;

        InfoClase(String nombre) {
            this.nombre = nombre;
        }

        int nextMethodIndex(String methodName) {
            int actual = indicesMetodos.getOrDefault(methodName, 0);
            indicesMetodos.put(methodName, actual + 1);
            return actual;
        }

        int nextConstructorIndex() {
            return indicesConstructores++;
        }
    }
}