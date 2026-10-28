package cunoc.compi2.alien_code.runner;


import rars.AssemblyException;
import rars.SimulationException;
import rars.api.Options;
import rars.api.Program;
import rars.simulator.Simulator;

import java.util.ArrayList;

/**
 * Ensambla y ejecuta un programa RISC-V (String) dentro de la aplicacion usando
 * la API de RARS 1.6.
 * Entrada: RARS recibe todo el stdin antes de ejecutar. Se le agrega al final una
 * linea con el byte 0x04 que el lector interpreta como fin de entrada.
 */
public final class RarsRunner {

    /**
     * Maximo de instrucciones simuladas (protege contra bucles infinitos).
     * RARS simula ~1.3 millones de instrucciones por segundo (medido), asi que
     * 30 millones son ~25 s. Ejecutar SIEMPRE en un hilo aparte (SwingWorker) en la UI.
     */
    public static final int MAX_PASOS = 30_000_000;

    public enum Estado {OK, ERROR_ENSAMBLADO, ERROR_EJECUCION, LIMITE_PASOS}

    public static final class Resultado {
        public final Estado estado;
        public final String salida;      // lo que imprimio el programa
        public final String mensaje;     // errores de ensamblado / ejecucion (puede ser vacio)
        public final int codigoSalida;

        Resultado(Estado estado, String salida, String mensaje, int codigoSalida) {
            this.estado = estado;
            this.salida = salida;
            this.mensaje = mensaje;
            this.codigoSalida = codigoSalida;
        }
    }

    private RarsRunner() {
    }

    public static Resultado ejecutar(String asm, String entrada) {
        return ejecutar(asm, entrada, MAX_PASOS);
    }

    public static Resultado ejecutar(String asm, String entrada, int maxPasos) {
        Options opciones = new Options();
        // OJO: con assembleString la API NO respeta startAtMain: la ejecucion empieza en la
        // PRIMERA instruccion de .text. El generador debe emitir el preambulo 'main:' primero
        // y dejar el runtime y cualquier otra rutina despues.
        opciones.startAtMain = true;
        opciones.maxSteps = maxPasos;
        Program programa = new Program(opciones);

        // Defensa: RARS falla con CRLF y con BOM (ocurre al escribir el .s en Windows).
        if (asm.startsWith("\uFEFF")) asm = asm.substring(1);
        asm = asm.replace("\r\n", "\n");

        try {
            programa.assembleString(asm);
        } catch (AssemblyException e) {
            // Un error aqui casi siempre es un bug del generador, no del usuario.
            return new Resultado(Estado.ERROR_ENSAMBLADO, "",
                    e.errors().generateErrorReport(), 1);
        } catch (Exception e) {
            return new Resultado(Estado.ERROR_ENSAMBLADO, "", String.valueOf(e), 1);
        }

        String stdin = entrada == null ? "" : entrada;
        if (!stdin.isEmpty() && !stdin.endsWith("\n")) stdin += "\n";
        stdin += "\u0004\n";               // centinela de fin de entrada

        programa.setup(new ArrayList<>(), stdin);

        try {
            Simulator.Reason razon = programa.simulate();
            Estado estado = (razon == Simulator.Reason.MAX_STEPS) ? Estado.LIMITE_PASOS : Estado.OK;
            String msg = (estado == Estado.LIMITE_PASOS)
                    ? "El programa excedio el limite de " + maxPasos + " instrucciones." : "";
            return new Resultado(estado, programa.getSTDOUT(), msg, programa.getExitCode());
        } catch (SimulationException e) {
            String msg;
            try {
                msg = e.error().getMessage();
            } catch (Exception ignorada) {
                msg = e.toString();
            }
            return new Resultado(Estado.ERROR_EJECUCION, programa.getSTDOUT(), msg, 1);
        } catch (Exception e) {
            return new Resultado(Estado.ERROR_EJECUCION, programa.getSTDOUT(), String.valueOf(e), 1);
        }
    }

    /**
     * Uso por consola (prueba rapida): java -cp rars.jar RarsRunner.java programa.s [entrada|@archivo]
     */
    public static void main(String[] args) throws Exception {
        String asm = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0])));
        String in = "";
        if (args.length > 1) {
            in = args[1].startsWith("@")
                    ? new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[1].substring(1))))
                    : args[1].replace("\\n", "\n");
        }
        Resultado r = ejecutar(asm, in);
        System.out.print(r.salida);
        System.err.println("[" + r.estado + " exit=" + r.codigoSalida + (r.mensaje.isEmpty() ? "" : " " + r.mensaje) + "]");
    }
}