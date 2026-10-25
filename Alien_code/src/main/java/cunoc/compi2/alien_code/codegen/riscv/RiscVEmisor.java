package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.cuartetas.Asignacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Condicional3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.DeclararArreglo3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Etiqueta3D;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Goto3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Halt3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Imprimir3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Leer3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Llamada3D;
import cunoc.compi2.alien_code.c3d.cuartetas.LlamadaVirtual3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Operacion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.Retornar3D;
import cunoc.compi2.alien_code.codegen.riscv.emisores.EmisorAritmetica;
import cunoc.compi2.alien_code.codegen.riscv.emisores.EmisorControl;
import cunoc.compi2.alien_code.codegen.riscv.emisores.EmisorES;
import cunoc.compi2.alien_code.codegen.riscv.emisores.EmisorLlamadas;
import cunoc.compi2.alien_code.codegen.riscv.emisores.EmisorMemoria;

public final class RiscVEmisor {
    private final ContextoRiscV ctx;
    private final EmisorMemoria memoria;
    private final EmisorAritmetica aritmetica;
    private final EmisorControl control;
    private final EmisorLlamadas llamadas;
    private final EmisorES es;

    public RiscVEmisor(ContextoRiscV ctx) {
        this.ctx = ctx;
        this.memoria = new EmisorMemoria(ctx);
        this.aritmetica = new EmisorAritmetica(ctx);
        this.control = new EmisorControl(ctx);
        this.llamadas = new EmisorLlamadas(ctx);
        this.es = new EmisorES(ctx);
    }

    public void emitir(Cuarteta cuarteta) {
        switch (cuarteta) {
            case Asignacion3D a -> memoria.asignar(a);
            case Operacion3D op -> aritmetica.operacion(op);
            case DeclararArreglo3D arr -> memoria.declararArreglo(arr);
            case Etiqueta3D et -> control.etiqueta(et);
            case Goto3D go -> control.salto(go);
            case Condicional3D cond -> control.condicional(cond);
            case Llamada3D lam -> llamadas.llamada(lam);
            case LlamadaVirtual3D virt -> llamadas.virtual(virt);
            case Imprimir3D imp -> es.imprimir(imp);
            case Leer3D leer -> es.leer(leer);
            case Retornar3D ret -> control.retornar(ret);
            case Halt3D halt -> control.detener(halt);
            case InicioFuncion3D inicio -> control.inicioFuncion(inicio);
            case FinFuncion3D fin -> control.finFuncion(fin);
            default -> {
                ctx.error("cuarteta no soportada: " + cuarteta.getClass().getSimpleName());
                ctx.emitir("    # FIXME cuarteta: " + cuarteta.getClass().getSimpleName());
            }
        }
    }
}