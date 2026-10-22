package cunoc.compi2.alien_code.errors;

public class CompilerError {
    private final ErrorType type;
    private final String message;
    private final int line;
    private final int column;
    private final String archivo;

    public CompilerError(ErrorType type, String message, int line, int column, String archivo) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.column = column;
        this.archivo = archivo;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getArchivo() {
        return archivo;
    }

    @Override
    public String toString() {
        String donde = archivo != null ? archivo + ": " : "";
        return String.format("%s[%s] Línea %d, columna %d: %s", donde, type, line, column, message);
    }
}
