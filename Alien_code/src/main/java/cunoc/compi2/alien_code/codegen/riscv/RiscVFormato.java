package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;

import java.util.ArrayList;
import java.util.List;

public final class RiscVFormato {
    public enum TipoTrozo {
        TEXTO, ESPECIFICADOR
    }

    public record Trozo(TipoTrozo tipo, String texto) {
    }

    public List<Trozo> descomponer(Imprimir3D imp) {
        String formato = imp.getFormato() == null ? "" : imp.getFormato().trim();
        if (formato.startsWith("\"")) {
            formato = formato.substring(1);
        }
        if (formato.endsWith("\"") && !formato.endsWith("\\\"") && !formato.isEmpty()) {
            formato = formato.substring(0, formato.length() - 1);
        }
        String dec = ContextoRiscV.decodificarEscapes(formato);

        List<Trozo> trozos = new ArrayList<>();
        StringBuilder texto = new StringBuilder();
        int i = 0;
        while (i < dec.length()) {
            char ch = dec.charAt(i);
            if (ch != '%') {
                texto.append(ch);
                i++;
                continue;
            }
            if (i + 1 < dec.length() && dec.charAt(i + 1) == '%') {
                texto.append('%');
                i += 2;
                continue;
            }
            int j = i + 1;
            while (j < dec.length() && !esEspecificador(dec.charAt(j))) {
                j++;
            }
            if (j >= dec.length()) {
                texto.append(ch);
                i++;
                continue;
            }
            if (texto.length() > 0) {
                trozos.add(new Trozo(TipoTrozo.TEXTO, texto.toString()));
                texto.setLength(0);
            }
            trozos.add(new Trozo(TipoTrozo.ESPECIFICADOR, "%" + dec.charAt(j)));
            i = j + 1;
        }
        if (texto.length() > 0) {
            trozos.add(new Trozo(TipoTrozo.TEXTO, texto.toString()));
        }
        return trozos;
    }

    private boolean esEspecificador(char ch) {
        return "diouxXeEfFgGaAcspn".indexOf(ch) >= 0;
    }
}
