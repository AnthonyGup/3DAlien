package cunoc.compi2.alien_code.ast.decl;

import cunoc.compi2.alien_code.ast.Expresion;
import cunoc.compi2.alien_code.ast.Type;
import cunoc.compi2.alien_code.ast.stmt.VariableDeclNode;
import cunoc.compi2.alien_code.c3d.access.CampoAccess;
import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;
import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.ir.ClaseLayout;
import cunoc.compi2.alien_code.ir.CodigoContexto;
import cunoc.compi2.alien_code.ir.IntermediateCodeGenerator;
import cunoc.compi2.alien_code.ir.NombresInternos;
import cunoc.compi2.alien_code.ir.Operandos;
import cunoc.compi2.alien_code.semantic.Symbol;

import java.util.List;

/**
 * Logica compartida de constructores (Fase 3): inicializadores de atributos
 * propios y busqueda del constructor del padre por aridad.
 */
public final class ConstructorSupport {

    private ConstructorSupport() {}

    public static void emitirInicializadores(CodigoContexto ctx, IntermediateCodeGenerator gen, String clase) {
        List<VariableDeclNode> atributos = gen.getAtributosClase();
        ClaseLayout layout = gen.getLayouts() != null ? gen.getLayouts().layoutDe(clase) : null;
        if (atributos == null || layout == null) {
            return;
        }
        for (VariableDeclNode attr : atributos) {
            if (attr.inicial == null) {
                continue;
            }
            int offset = layout.offsetCampo(attr.nombre);
            if (offset < 0) {
                continue;
            }
            MemoryAccess valor = ((Expresion) attr.inicial).traducir(ctx);
            MemoryAccess base = new Literal3D("self", Type.CLASS);
            String tipoNombre = attr.tipoNombre;
            int dimensiones = attr.tipo == Type.ARRAY ? attr.dimensiones : 0;
            CampoAccess campo = new CampoAccess(base, attr.nombre,
                    attr.tipo == Type.ARRAY && attr.tipoElemento != null ? attr.tipoElemento : attr.tipo,
                    tipoNombre, dimensiones);
            campo.setOffset(offset);
            if (valor instanceof Literal3D lit) {
                valor = Operandos.literalBraces(campo, lit);
            }
            ctx.agregar(new Asignacion3D(campo, valor));
        }
    }

    public static Symbol buscarConstructor(Symbol clase, int aridad) {
        if (clase == null) {
            return null;
        }
        for (Symbol ctor : clase.getConstructores()) {
            if (ctor.getTiposParametros().size() == aridad) {
                return ctor;
            }
        }
        return null;
    }

    public static void emitirConstructorSintetico(CodigoContexto ctx, String clase) {
        IntermediateCodeGenerator gen = (IntermediateCodeGenerator) ctx;
        String etiqueta = NombresInternos.constructor(clase, 0);
        ctx.agregar(new InicioFuncion3D(etiqueta, "self:" + clase, clase, "void"));
        Symbol claseSimbolo = gen.getLayouts() != null ? gen.getLayouts().simboloDe(clase) : null;
        Symbol padre = claseSimbolo != null ? claseSimbolo.getSuperclaseResuelta() : null;
        if (padre != null) {
            String etiquetaPadre = NombresInternos.constructor(padre.getName(), 0);
            java.util.List<MemoryAccess> args = new java.util.ArrayList<>();
            args.add(new Literal3D("self", Type.CLASS));
            ctx.agregar(new Llamada3D(etiquetaPadre, args, null));
        }
        emitirInicializadores(ctx, gen, clase);
        ctx.agregar(new FinFuncion3D(etiqueta));
    }
}
