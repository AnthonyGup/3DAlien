package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.ir.ClaseLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuarteta abstracta de despacho dinamico (Fase 3, seccion 6).
 * Representa "slot S sobre el objeto receptor": el backend decide, segun la
 * cabecera del objeto (id de clase), que implementacion invocar.
 */
public class LlamadaVirtual3D extends Cuarteta {
    private final MemoryAccess receptor;
    private final int slot;
    private final String metodo;
    private final List<MemoryAccess> argumentos;
    private final MemoryAccess destino;
    private final List<ClaseLayout.ImplMetodo> candidatos;
    private int callerFrameSize = -1;
    private boolean usuario = true;
    private boolean tieneSelf = true;
    private boolean retornaValor;

    public LlamadaVirtual3D(MemoryAccess receptor, int slot, String metodo,
            List<MemoryAccess> argumentos, MemoryAccess destino,
            List<ClaseLayout.ImplMetodo> candidatos) {
        this.receptor = receptor;
        this.slot = slot;
        this.metodo = metodo;
        this.argumentos = argumentos == null ? new ArrayList<>() : argumentos;
        this.destino = destino;
        this.candidatos = candidatos == null ? new ArrayList<>() : candidatos;
        this.retornaValor = destino != null;
    }

    public MemoryAccess getReceptor() { return receptor; }
    public int getSlot() { return slot; }
    public String getMetodo() { return metodo; }
    public List<MemoryAccess> getArgumentos() { return argumentos; }
    public MemoryAccess getDestino() { return destino; }
    public List<ClaseLayout.ImplMetodo> getCandidatos() { return candidatos; }

    public void setCallerFrameSize(int v) { this.callerFrameSize = v; }
    public int getCallerFrameSize() { return callerFrameSize; }
    public void setUsuario(boolean v) { this.usuario = v; }
    public boolean isUsuario() { return usuario; }
    public void setTieneSelf(boolean v) { this.tieneSelf = v; }
    public boolean isTieneSelf() { return tieneSelf; }
    public boolean isRetornaValor() { return retornaValor; }

    /** Mapa idDeClase -> etiqueta, para el switch del backend. */
    public Map<Integer, String> tablaDespacho() {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        for (ClaseLayout.ImplMetodo impl : candidatos) {
            mapa.put(impl.claseId, impl.etiqueta);
        }
        return mapa;
    }

    @Override
    public String operator() {
        return "callvirt";
    }

    @Override
    public MemoryAccess getOperand1() {
        return receptor;
    }

    @Override
    public MemoryAccess getOperand2() {
        StringBuilder texto = new StringBuilder();
        texto.append("slot ").append(slot).append(" (").append(metodo).append(')');
        for (MemoryAccess a : argumentos) {
            texto.append(", ").append(render(a));
        }
        return new Literal3D(texto.toString(), null);
    }

    @Override
    public MemoryAccess getResult() {
        return destino;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        if (destino != null) {
            sb.append(render(destino)).append(" = ");
        }
        sb.append("vcall[").append(slot).append("].").append(metodo).append('(');
        sb.append(render(receptor));
        for (MemoryAccess a : argumentos) {
            sb.append(", ").append(render(a));
        }
        sb.append(");");
    }
}
