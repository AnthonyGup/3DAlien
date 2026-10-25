package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.Symbol;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.util.List;
import java.util.Map;

public final class RiscVCodeGenerator {
    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales) {
        return generate(instrucciones, tabla, tiposTemporales, null);
    }

    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales,
            ClaseLayouts layouts) {
        List<Cuarteta> todas = instrucciones == null ? List.of() : instrucciones;

        ContextoRiscV ctx = new ContextoRiscV(todas, tabla, tiposTemporales, layouts);
        ctx.indexar();
        ctx.asignarRetIds();

        // Calcular offsets de bytes para todos los operandos
        asignarOffsets(ctx);

        RiscVEmisor emisor = new RiscVEmisor(ctx);
        new GeneradorCuerpo(ctx, emisor).generar(todas);

        emitirDatos(ctx);

        StringBuilder salida = new StringBuilder();
        salida.append("# St4ck Over-Pig - RISC-V RV32IMFD (RARS 1.6) - convención clase\n");
        salida.append(ctx.datos());
        salida.append('\n');
        salida.append(".text\n");
        salida.append(ctx.texto());
        return salida.toString();
    }

    private void asignarOffsets(ContextoRiscV ctx) {
        // 1. Parámetros y locales de cada función (ya asignados en EmisorControl.inicioFuncion)
        // El marco actual se actualiza al entrar a cada función

        // 2. Globales ya tienen offset en ctx.getOffsetGlobal()

        // 3. Temporales y locales: asignar en cada marco
        for (var marco : ctx.marcos().values()) {
            // Los offsets ya se asignan durante la generación (EmisorControl.inicioFuncion)
        }
    }

    private void emitirDatos(ContextoRiscV ctx) {
        ctx.emitirDato(".data");
        ctx.emitirDato(".align 3");

        // Globals
        int globalsSize = ctx.totalGlobalesBytes();
        if (globalsSize > 0) {
            ctx.emitirDato("Globals: .space " + globalsSize);
        } else {
            ctx.emitirDato("Globals: .space 16");
        }

        // Heap (3 MB)
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Heap: .space 3145728");

        // Cadenas (cada una con su etiqueta)
        for (var e : ctx.etiquetasCadena().entrySet()) {
            String literal = e.getKey();
            String et = e.getValue();
            String contenido = literal.substring(1, literal.length() - 1); // quitar comillas
            // Manejar escapes: \n, \t, \", \\
            contenido = contenido
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            // Verificar si es ASCII seguro
            boolean ascii = true;
            for (char ch : contenido.toCharArray()) {
                if (ch < 32 || ch >= 127) { ascii = false; break; }
            }
            if (ascii) {
                ctx.emitirDato(et + ": .asciz \"" + contenido + "\"");
            } else {
                // Emitir como bytes
                StringBuilder sb = new StringBuilder();
                sb.append(et + ": .byte ");
                for (int i = 0; i < contenido.length(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append((int) contenido.charAt(i));
                }
                sb.append(", 0");
                ctx.emitirDato(sb.toString());
            }
        }

        // Constantes double
        ctx.emitirDato(".align 3");
        for (var e : ctx.etiquetasConstante().entrySet()) {
            ctx.emitirDato(e.getValue() + ": .double " + e.getKey());
        }

        // Formatos
        for (var e : ctx.etiquetasFormato().entrySet()) {
            String literal = e.getKey();
            String et = e.getValue();
            String contenido = literal;
            // Sin comillas externas, ya procesado
            boolean ascii = true;
            for (char ch : contenido.toCharArray()) {
                if (ch < 32 || ch >= 127) { ascii = false; break; }
            }
            if (ascii) {
                ctx.emitirDato(et + ": .asciz \"" + contenido + "\"");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(et + ": .byte ");
                for (int i = 0; i < contenido.length(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append((int) contenido.charAt(i));
                }
                sb.append(", 0");
                ctx.emitirDato(sb.toString());
            }
        }

        // Área libre de cadenas (128 KB)
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Str_libre: .space 131072");
        ctx.emitirDato(".align 3");
    }

    public List<String> errores() {
        return List.of();
    }
}