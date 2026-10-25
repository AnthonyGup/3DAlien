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
        if (formato.endsWith("\"") && !formato.endsWith("\\\"")) {
            formato = formato.substring(0, formato.length() - 1);
        }
        List<Trozo> trozos = new ArrayList<>();
        StringBuilder texto = new StringBuilder();
        int i = 0;
        while (i < formato.length()) {
            char ch = formato.charAt(i);
            if (ch != '%') {
                texto.append(ch);
                i++;
                continue;
            }
            if (i + 1 < formato.length() && formato.charAt(i + 1) == '%') {
                texto.append('%');
                i += 2;
                continue;
            }
            int j = i + 1;
            while (j < formato.length() && !esEspecificador(formato.charAt(j))) {
                j++;
            }
            if (j >= formato.length()) {
                texto.append(ch);
                i++;
                continue;
            }
            if (texto.length() > 0) {
                trozos.add(new Trozo(TipoTrozo.TEXTO, texto.toString()));
                texto.setLength(0);
            }
            trozos.add(new Trozo(TipoTrozo.ESPECIFICADOR, "%" + formato.charAt(j)));
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
