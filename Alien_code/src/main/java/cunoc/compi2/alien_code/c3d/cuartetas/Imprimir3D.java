package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.access.Literal3D;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

import java.util.List;

public class Imprimir3D extends Cuarteta {
    private final String funcion;
    private final String formato;
    private final String args;
    private List<MemoryAccess> operandos;
    private List<String> especificadores;

    public Imprimir3D(String funcion, String formato, String args) {
        this.funcion = funcion;
        this.formato = formato;
        this.args = args;
    }

    public String getFormato() {
        return formato;
    }

    public String getArgs() {
        return args;
    }

    public List<MemoryAccess> getOperandos() { return operandos; }
    public void setOperandos(List<MemoryAccess> operandos) { this.operandos = operandos; }

    public List<String> getEspecificadores() { return especificadores; }
    public void setEspecificadores(List<String> especificadores) { this.especificadores = especificadores; }

    @Override
    public String operator() {
        return funcion;
    }

    @Override
    public MemoryAccess getOperand1() {
        return new Literal3D(formato, null);
    }

    @Override
    public MemoryAccess getOperand2() {
        return args == null || args.isEmpty() ? null : new Literal3D(args, null);
    }

    @Override
    public MemoryAccess getResult() {
        return null;
    }

    @Override
    public void toCCode(StringBuilder sb) {
        sb.append(funcion).append('(').append(formato);
        if (args != null && !args.isEmpty()) {
            sb.append(", ").append(args);
        }
        sb.append(");");
    }
}