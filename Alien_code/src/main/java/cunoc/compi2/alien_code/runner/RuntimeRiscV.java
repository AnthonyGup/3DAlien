package cunoc.compi2.alien_code.runner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Carga el runtime RISC-V (/riscv/runtime.s) empaquetado como recurso y lo
 * entrega como texto para agregarlo al final de todo programa generado.
 */
public final class RuntimeRiscV {

    private static String cache;

    private RuntimeRiscV() {
    }

    public static synchronized String texto() {
        if (cache == null) {
            try (InputStream in = RuntimeRiscV.class.getResourceAsStream("/riscv/runtime.s")) {
                if (in == null) {
                    throw new IllegalStateException("Falta el recurso /riscv/runtime.s");
                }
                cache = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer /riscv/runtime.s", e);
            }
        }
        return cache;
    }
}
