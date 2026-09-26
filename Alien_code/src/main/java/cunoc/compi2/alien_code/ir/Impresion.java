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
            if (tipo == Type.BOOL) {
                formato.append("%s");
                partes.add("(" + textoArg + " ? \"true\" : \"false\")");
            } else if (tipo == Type.STRING) {
                formato.append("%s");
                partes.add(textoArg);
            } else if (tipo == Type.CHAR) {
                formato.append("%c");
                partes.add(textoArg);
            } else if (tipo == Type.FLOAT) {
                formato.append("%g");
                partes.add(textoArg);
            } else if (tipo == Type.STRUCT || tipo == Type.CLASS) {
                formato.append("%p");
                partes.add(textoArg);
            } else {
                formato.append("%d");
                partes.add(textoArg);
            }
        }
        String argsTexto = partes.isEmpty() ? null : String.join(", ", partes);
        return new Imprimir3D("printf", "\"" + formato + "\"", argsTexto);
    }
}