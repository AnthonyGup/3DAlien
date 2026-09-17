package cunoc.compi2.alien_code.semantic;

import cunoc.compi2.alien_code.ast.Node;
import cunoc.compi2.alien_code.ast.Type;

public interface ContextoSemantico {
    Type evaluar(Node nodo);

    void entrarAmbito();

    void salirAmbito();

    boolean definir(Symbol simbolo);

    Symbol resolver(String nombre);

    void registrarError(int linea, int columna, String mensaje);

    void entrarCiclo();

    void salirCiclo();

    boolean enCiclo();
}