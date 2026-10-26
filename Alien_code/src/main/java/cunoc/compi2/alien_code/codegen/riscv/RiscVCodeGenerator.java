package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.ir.ClaseLayouts;
import cunoc.compi2.alien_code.semantic.SymbolTable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class RiscVCodeGenerator {
    private List<String> errores = List.of();

    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales) {
        return generate(instrucciones, tabla, tiposTemporales, null);
    }

    public String generate(List<Cuarteta> instrucciones, SymbolTable tabla, Map<Integer, String> tiposTemporales,
            ClaseLayouts layouts) {
        List<Cuarteta> todas = instrucciones == null ? List.of() : instrucciones;

        ContextoRiscV ctx = new ContextoRiscV(todas, tabla, tiposTemporales, layouts);
        ctx.indexar();

        RiscVEmisor emisor = new RiscVEmisor(ctx);
        new GeneradorCuerpo(ctx, emisor).generar(todas);

        errores = ctx.errores();
        emitirDatos(ctx);
        emitirStubs(ctx);

        StringBuilder salida = new StringBuilder();
        salida.append("# 3DAlien - RISC-V RV32IMFD (RARS 1.6)\n");
        salida.append(ctx.datos());
        salida.append('\n');
        salida.append(".text\n");
        salida.append(ctx.texto());
        return salida.toString();
    }

    private void emitirDatos(ContextoRiscV ctx) {
        ctx.emitirDato(".data");
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Globals: .space " + Math.max(16, ctx.totalGlobalesBytes()));
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Heap: .space 3145728");
        ctx.emitirDato(".align 3");
        ctx.emitirDato("Str_libre: .space 131072");

        for (var e : ctx.etiquetasCadena().entrySet()) {
            emitirBytes(ctx, e.getValue(), e.getKey());
        }
        for (var e : ctx.etiquetasFormato().entrySet()) {
            emitirBytes(ctx, e.getValue(), e.getKey());
        }
        if (!ctx.etiquetasConstante().isEmpty()) {
            ctx.emitirDato(".align 3");
            for (var e : ctx.etiquetasConstante().entrySet()) {
                ctx.emitirDato(e.getValue() + ": .double " + e.getKey());
            }
        }
    }

    private void emitirBytes(ContextoRiscV ctx, String etiqueta, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(etiqueta).append(": .byte ");
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(bytes[i] & 0xFF);
        }
        if (bytes.length == 0) {
            sb.append("0");
        } else {
            sb.append(", 0");
        }
        ctx.emitirDato(sb.toString());
    }

    private void emitirStubs(ContextoRiscV ctx) {
        for (String fn : ctx.getLlamadasUsuario()) {
            if ("main".equals(fn) || ctx.getFuncionesDefinidas().contains(fn)) continue;
            ctx.error("Funcion '" + fn + "' no definida: se genera un stub que devuelve 0");
            String et = ctx.etiquetaFuncion(fn);
            ctx.emitir(et + ":");
            ctx.emitir("    li a0, 0");
            ctx.emitir("    fcvt.d.w fa0, zero");
            ctx.emitir("    ret");
        }
    }

    public List<String> errores() {
        return errores;
    }
}
