package cunoc.compi2.alien_code.c3d;

public class C3DInstruction {
    public String operador;
    public String operando1;
    public String operando2;
    public String resultado;

    public C3DInstruction(String operador, String operando1, String operando2, String resultado) {
        this.operador = operador;
        this.operando1 = operando1;
        this.operando2 = operando2;
        this.resultado = resultado;
    }

    @Override
    public String toString() {
        if (resultado == null || resultado.isEmpty()) {
            return String.format("%s %s %s;", operador, operando1, operando2);
        }
        return String.format("%s = %s %s %s;", resultado, operando1, operador, operando2);
    }
}
