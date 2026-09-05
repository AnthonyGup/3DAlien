package cunoc.compi2.alien_code.errors;

import java.util.ArrayList;
import java.util.List;

public class ErrorListener {
    private final List<CompilerError> errors;

    public ErrorListener() {
        this.errors = new ArrayList<>();
    }

    public void addError(ErrorType type, String message, int line, int column) {
        errors.add(new CompilerError(type, message, line, column));
    }

    public List<CompilerError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clear() {
        errors.clear();
    }

    public String getFormattedErrors() {
        StringBuilder sb = new StringBuilder();
        for (CompilerError error : errors) {
            sb.append(error.toString()).append("\n");
        }
        return sb.toString();
    }
}
