package cunoc.compi2.alien_code.ir;

import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;

import java.util.ArrayList;
import java.util.List;

public final class Impresion {
    private Impresion() {
    }

    public static Imprimir3D print(CodigoContexto ctx, List<MemoryAccess> argumentos) {
        StringBuilder formato = new StringBuilder();
        List<String> partes = new ArrayList<>();
        List<MemoryAccess> operandos = new ArrayList<>();
        List<String> especificadores = new ArrayList<>();
        for (MemoryAccess arg : argumentos) {
            if (arg instanceof Literal3D literal && literal.getTipo() == Type.STRING) {
                String texto = literal.getTexto();
                if (texto.length() >= 2) {
                    formato.append(texto, 1, texto.length() - 1);
                }
                continue;
            }
            Type tipo = Operandos.tipoOperando(ctx, arg);
            String textoArg = Operandos.texto(arg);
            String especificador;
            if (tipo == Type.BOOL) {
                especificador = "%s";
                partes.add("(" + textoArg + " ? \"true\" : \"false\")");
            } else if (tipo == Type.STRING) {
                especificador = "%s";
                partes.add(textoArg);
            } else if (tipo == Type.CHAR) {
                especificador = "%c";
                partes.add(textoArg);
            } else if (tipo == Type.FLOAT) {
                especificador = "%g";
                partes.add(textoArg);
            } else if (tipo == Type.STRUCT || tipo == Type.CLASS) {
                especificador = "%p";
                partes.add(textoArg);
            } else {
                especificador = "%d";
                partes.add(textoArg);
            }
            formato.append(especificador);
            operandos.add(arg);
            especificadores.add(especificador);
        }
        String argsTexto = partes.isEmpty() ? null : String.join(", ", partes);
        Imprimir3D imp = new Imprimir3D("printf", "\"" + formato + "\"", argsTexto);
        imp.setOperandos(operandos);
        imp.setEspecificadores(especificadores);
        return imp;
    }
}