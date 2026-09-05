package cunoc.compi2.alien_code.errors;

public class CompilerError {
    private final ErrorType type;
    private final String message;
    private final int line;
    private final int column;

    public CompilerError(ErrorType type, String message, int line, int column) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public ErrorType getType() {
        return type;
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

    @Override
    public String toString() {
        return String.format("[%s] Línea %d, columna %d: %s", type, line, column, message);
    }
}
