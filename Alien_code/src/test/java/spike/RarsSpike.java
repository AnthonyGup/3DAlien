import rars.AssemblyException;
import rars.SimulationException;
import rars.api.Options;
import rars.api.Program;
import rars.simulator.Simulator;

import java.util.ArrayList;

/**
 * Fase 0 - spike de RARS 1.6 embebido (rars.api.Program).
 * Verifica: ensamblado desde String, E/S (int, double, string, char), fld/fadd.d,
 * captura de excepciones y limite de memoria con .space.
 */
public class RarsSpike {

    private static int fallos = 0;

    public static void main(String[] args) {
        System.out.println("=== FASE 0: SPIKE RARS 1.6 ===");

        spikeBase();
        spikeLeerString();
        spikeLeerChar();
        spikeLeerCharTrasInt();
        spikeErrores();
        spikeMemoria();

        System.out.println();
        System.out.println(fallos == 0
                ? "RESULTADO: todos los spikes OK"
                : "RESULTADO: " + fallos + " fallo(s)");
    }

    // ------------------------------------------------------------------
    // 1. Base: string + leer int + leer double + fadd.d + imprimir
    //    Esperado: reason=NORMAL_TERMINATION, exit=0,
    //              STDOUT=[Hola Mundo\n6.0\n42]
    // ------------------------------------------------------------------
    private static void spikeBase() {
        System.out.println("\n--- 1) base ---");
        String asm = String.join("\n",
                ".data",
                "msg: .asciz \"Hola Mundo\\n\"",
                "dbl: .double 2.5",
                ".text",
                "main:",
                "  la a0, msg", "  li a7, 4", "  ecall",
                "  li a7, 5", "  ecall",
                "  mv t0, a0",
                "  li a7, 7", "  ecall",
                "  fmv.d ft0, fa0",
                "  la t1, dbl", "  fld ft1, 0(t1)",
                "  fadd.d fa0, ft0, ft1",
                "  li a7, 3", "  ecall",
                "  li a0, 10", "  li a7, 11", "  ecall",
                "  mv a0, t0", "  li a7, 1", "  ecall",
                "  li a7, 10", "  ecall");

        String salida = ejecutar(asm, "42\n3.5\n");
        String esperado = "Hola Mundo\n6.0\n42";
        comparar("base", esperado, salida);
    }

    // ------------------------------------------------------------------
    // 2. ecall 8 (leer string en buffer)
    // ------------------------------------------------------------------
    private static void spikeLeerString() {
        System.out.println("\n--- 2) ecall 8: leer string ---");
        String asm = String.join("\n",
                ".data",
                "buf: .space 32",
                ".text",
                "main:",
                "  la a0, buf", "  li a1, 31", "  li a7, 8", "  ecall",
                "  la a0, buf", "  li a7, 4", "  ecall",
                "  li a0, 10", "  li a7, 11", "  ecall",
                "  li a7, 10", "  ecall");

        String salida = ejecutar(asm, "Hola RARS\n");
        // Se imprime lo leido (que RARS deja con el salto de linea) + el salto extra
        System.out.println("  STDOUT = [" + salida.replace("\n", "\\n") + "]");
        System.out.println("  nota: ecall 8 guarda la linea tal cual, incluido '\\n'");
    }

    // ------------------------------------------------------------------
    // 3. ecall 12 (leer un char)
    // ------------------------------------------------------------------
    private static void spikeLeerChar() {
        System.out.println("\n--- 3) ecall 12: leer char ---");
        String asm = String.join("\n",
                ".data",
                ".text",
                "main:",
                "  li a7, 12", "  ecall", "   # lee char -> a0",
                "  mv t0, a0",
                "  li a7, 11", "  ecall", "   # imprime el char",
                "  li a0, 10", "  li a7, 11", "  ecall",
                "  mv a0, t0", "  li a7, 1", "  ecall", "   # imprime el codigo",
                "  li a7, 10", "  ecall");

        String salida = ejecutar(asm, "Z\n");
        comparar("leer char", "Z\n90", salida);
    }

    // ------------------------------------------------------------------
    // 4. ecall 12 justo despues de ecall 5 (leer int): ¿come el '\n'?
    //    Es el caso que nos importa para el ahorcado (letra tras un numero).
    // ------------------------------------------------------------------
    private static void spikeLeerCharTrasInt() {
        System.out.println("\n--- 4) ecall 12 despues de ecall 5 (¿come el \\n?) ---");
        String asm = String.join("\n",
                ".data",
                ".text",
                "main:",
                "  li a7, 5", "  ecall", "   # lee int -> a0",
                "  mv t0, a0",
                "  li a7, 12", "  ecall", "   # lee char -> a0",
                "  mv t1, a0",
                "  mv a0, t0", "  li a7, 1", "  ecall", "   # imprime el int",
                "  li a0, 10", "  li a7, 11", "  ecall",
                "  mv a0, t1", "  li a7, 11", "  ecall", "   # imprime el char",
                "  li a0, 10", "  li a7, 11", "  ecall",
                "  mv a0, t1", "  li a7, 1", "  ecall", "   # imprime su codigo",
                "  li a7, 10", "  ecall");

        String salida = ejecutar(asm, "7\nZ\n");
        System.out.println("  STDOUT = [" + salida.replace("\n", "\\n") + "]");
        System.out.println("  nota: si el char impreso es 'Z'(90) el \\n lo salta;");
        System.out.println("        si es 10, hay que saltarlo en el runtime (como el \" %c\" de C)");
    }

    // ------------------------------------------------------------------
    // 5. Comportamiento ante errores: AssemblyException y SimulationException
    // ------------------------------------------------------------------
    private static void spikeErrores() {
        System.out.println("\n--- 5) errores ---");

        // 5a) error de ensamblado -> AssemblyException
        String mala = String.join("\n",
                ".text", "main:", "  li a7, 9999", "  no_existe_tal_instruccion", "  ecall");
        try {
            Program p = new Program(new Options());
            p.assembleString(mala);
            System.out.println("  [FALLO] no lanzo AssemblyException");
            fallos++;
        } catch (AssemblyException e) {
            System.out.println("  AssemblyException capturada OK");
            for (String linea : mensajesDe(e)) {
                System.out.println("    " + linea);
            }
        } catch (Exception e) {
            System.out.println("  [FALLO] excepcion inesperada: " + e);
            fallos++;
        }

        // 5b) bucle infinito -> MAX_STEPS (proteccion)
        String bucle = String.join("\n",
                ".text", "main:", "et:", "  j et");
        try {
            Options o = new Options();
            o.startAtMain = true;
            o.maxSteps = 100_000;
            Program p = new Program(o);
            p.assembleString(bucle);
            p.setup(new ArrayList<>(), "");
            Simulator.Reason r = p.simulate();
            System.out.println("  bucle infinito -> reason = " + r
                    + (r == Simulator.Reason.MAX_STEPS ? " (OK, protegido)" : " (inesperado)"));
        } catch (Exception e) {
            System.out.println("  [nota] bucle: " + e);
        }

        // 5c) acceso invalido a memoria -> SimulationException
        String malMem = String.join("\n",
                ".text", "main:", "  li t0, 0x7fffffff", "  lw t1, 0(t0)");
        try {
            Options o = new Options();
            o.startAtMain = true;
            o.maxSteps = 100_000;
            Program p = new Program(o);
            p.assembleString(malMem);
            p.setup(new ArrayList<>(), "");
            p.simulate();
            System.out.println("  [nota] acceso a memoria invalido no lanzo");
        } catch (SimulationException e) {
            System.out.println("  SimulationException capturada OK (acceso a memoria invalido)");
        } catch (Exception e) {
            System.out.println("  [nota] acceso memoria: " + e.getClass().getSimpleName()
                    + " -> " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 6. Presupuesto de memoria: .space grande y escritura en el ultimo byte
    //    Medido en RARS 1.6: 3.5 MB OK, 4 MB falla.
    // ------------------------------------------------------------------
    private static void spikeMemoria() {
        System.out.println("\n--- 6) memoria (.space) ---");
        int[] pruebas = {1 << 20, 2 << 20, 3 << 20, (int) (3.5 * 1024 * 1024), 4 << 20};
        for (int n : pruebas) {
            String asm = String.join("\n",
                    ".data",
                    "buf: .space " + n,
                    ".text",
                    "main:",
                    "  la t0, buf",
                    "  li t2, " + (n - 1),
                    "  add t0, t0, t2",
                    "  li t1, 7",
                    "  sb t1, 0(t0)",
                    "  lb a0, 0(t0)",
                    "  li a7, 1", "  ecall",
                    "  li a0, 10", "  li a7, 11", "  ecall",
                    "  li a7, 10", "  ecall");
            String etiqueta = String.format("%6.2f MB", n / 1024.0 / 1024.0);
            try {
                String salida = ejecutar(asm, "");
                System.out.println("  " + etiqueta + " -> OK (ultimo byte = "
                        + salida.trim() + ")");
            } catch (Exception e) {
                System.out.println("  " + etiqueta + " -> FALLO ("
                        + e.getClass().getSimpleName() + ")");
            }
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private static String ejecutar(String asm, String stdin) {
        try {
            Options opt = new Options();
            opt.startAtMain = true;
            opt.maxSteps = 10_000_000;
            Program p = new Program(opt);
            p.assembleString(asm);
            p.setup(new ArrayList<>(), stdin);
            Simulator.Reason razon = p.simulate();
            String out = p.getSTDOUT();
            System.out.println("  reason = " + razon + ", exit = " + p.getExitCode());
            if (p.getSTDERR() != null && !p.getSTDERR().isEmpty()) {
                System.out.println("  STDERR = [" + p.getSTDERR().replace("\n", "\\n") + "]");
            }
            return out == null ? "" : out.replace("\r\n", "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** AssemblyException (extends Exception) expone los errores via errors(). */
    private static java.util.List<String> mensajesDe(AssemblyException e) {
        java.util.List<String> out = new ArrayList<>();
        try {
            String reporte = e.errors().generateErrorReport();
            if (reporte != null && !reporte.isBlank()) {
                for (String linea : reporte.split("\n")) {
                    if (!linea.isBlank()) {
                        out.add(linea.trim());
                    }
                }
            }
        } catch (Exception ignored) {
            // sin errors() o fallo al invocarlo
        }
        if (out.isEmpty()) {
            out.add(String.valueOf(e));
        }
        return out;
    }

    private static void comparar(String nombre, String esperado, String obtenido) {
        if (esperado.equals(obtenido)) {
            System.out.println("  OK: " + nombre);
        } else {
            System.out.println("  [FALLO] " + nombre);
            System.out.println("    esperado = [" + esperado.replace("\n", "\\n") + "]");
            System.out.println("    obtenido = [" + obtenido.replace("\n", "\\n") + "]");
            fallos++;
        }
    }
}
