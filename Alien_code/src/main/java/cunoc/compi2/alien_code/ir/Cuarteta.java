package cunoc.compi2.alien_code.ir;

public class Cuarteta {
    public String operador;
    public String operando1;
    public String operando2;
    public String resultado;

    public Cuarteta(String operador, String operando1, String operando2, String resultado) {
        this.operador = operador;
        this.operando1 = operando1;
        this.operando2 = operando2;
        this.resultado = resultado;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s, %s)", operador, operando1, operando2, resultado);
    }
}
