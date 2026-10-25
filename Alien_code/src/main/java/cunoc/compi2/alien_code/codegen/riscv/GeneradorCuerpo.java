package cunoc.compi2.alien_code.codegen.riscv;

import cunoc.compi2.alien_code.c3d.cuartetas.Cuarteta;
import cunoc.compi2.alien_code.c3d.cuartetas.FinFuncion3D;
import cunoc.compi2.alien_code.c3d.cuartetas.InicioFuncion3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GeneradorCuerpo {
    private final ContextoRiscV ctx;
    private final RiscVEmisor emisor;

    public GeneradorCuerpo(ContextoRiscV ctx, RiscVEmisor emisor) {
        this.ctx = ctx;
        this.emisor = emisor;
    }

    public void generar(List<Cuarteta> todas) {
        List<Cuarteta> topLevel = new ArrayList<>();
        int i = 0;
        while (i < todas.size()) {
            Cuarteta actual = todas.get(i);
            if (actual instanceof InicioFuncion3D inicio) {
                int inicioCuerpo = i + 1;
                int fin = inicioCuerpo;
                while (fin < todas.size() && !(todas.get(fin) instanceof FinFuncion3D)) {
                    fin++;
                }
                ctx.setMarcoActual(ctx.marcos().get(inicio.getNombre()));
                for (int k = inicioCuerpo; k < fin; k++) {
                    emisor.emitir(todas.get(k));
                }
                i = fin + 1;
            } else {
                topLevel.add(actual);
                i++;
            }
        }

        // main ya emitió su preámbulo en EmisorControl.inicioFuncion
        for (Cuarteta cuarteta : topLevel) {
            emisor.emitir(cuarteta);
        }
        }
}