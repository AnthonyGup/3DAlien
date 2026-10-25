package cunoc.compi2.alien_code.codegen.riscv.datos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class PoolCadenas extends PoolBase {
    private static final byte[] TERMINADOR = {0};

    @Override
    protected void alRegistrar(String texto, int indice) {
        siguiente = indice + bytes(texto).length + 1;
    }

    public static String contenido(String literal) {
        if (literal == null || literal.length() < 2) {
            return "";
        }
        if (literal.startsWith("\"")) {
            int fin = literal.lastIndexOf('"');
            if (fin > 0) {
                return literal.substring(1, fin);
            }
            return literal.substring(1);
        }
        return literal;
    }

    public static byte[] bytes(String literal) {
        String crudo = contenido(literal);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int i = 0;
        while (i < crudo.length()) {
            char ch = crudo.charAt(i);
            if (ch == '\\' && i + 1 < crudo.length()) {
                char sig = crudo.charAt(i + 1);
                switch (sig) {
                    case 'n' -> {
                        salida.write('\n');
                        i += 2;
                        continue;
                    }
                    case 't' -> {
                        salida.write('\t');
                        i += 2;
                        continue;
                    }
                    case 'r' -> {
                        salida.write('\r');
                        i += 2;
                        continue;
                    }
                    case '"' -> {
                        salida.write('"');
                        i += 2;
                        continue;
                    }
                    case '\\' -> {
                        salida.write('\\');
                        i += 2;
                        continue;
                    }
                    case '\'' -> {
                        salida.write('\'');
                        i += 2;
                        continue;
                    }
                    case '0' -> {
                        salida.write(0);
                        i += 2;
                        continue;
                    }
                    default -> {
                        salida.write('\\');
                        salida.write(sig);
                        i += 2;
                        continue;
                    }
                }
            }
            byte[] trozo = String.valueOf(ch).getBytes(StandardCharsets.UTF_8);
            salida.write(trozo, 0, trozo.length);
            i++;
        }
        return salida.toByteArray();
    }

    public static boolean ascizSeguro(String literal) {
        String crudo = contenido(literal);
        for (int i = 0; i < crudo.length(); i++) {
            char ch = crudo.charAt(i);
            if (ch == '\\') {
                if (i + 1 >= crudo.length()) {
                    return false;
                }
                char sig = crudo.charAt(i + 1);
                if (sig != 'n' && sig != 't' && sig != '"' && sig != '\\') {
                    return false;
                }
                i++;
                continue;
            }
            if (ch == '"' || ch < 32 || ch > 126) {
                return false;
            }
        }
        return true;
    }

    public static String asciz(String texto) {
        return ".asciz \"" + texto + "\"";
    }

    public static String byteLine(String literal) {
        byte[] contenido = bytes(literal);
        StringBuilder sb = new StringBuilder(".byte ");
        for (int i = 0; i < contenido.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Byte.toUnsignedInt(contenido[i]));
        }
        if (contenido.length > 0) {
            sb.append(',');
        }
        sb.append('0');
        return sb.toString();
    }

    public static byte[] terminador() {
        return TERMINADOR.clone();
    }
}
