package cunoc.compi2.alien_code.c3d.cuartetas;

import cunoc.compi2.alien_code.c3d.CodeTransformable;
import cunoc.compi2.alien_code.c3d.access.MemoryAccess;

public abstract class Cuarteta implements CodeTransformable {
    public abstract String operator();

    public abstract MemoryAccess getOperand1();

    public abstract MemoryAccess getOperand2();

    public abstract MemoryAccess getResult();

    protected static String render(MemoryAccess access) {
        if (access == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        access.toCCode(sb);
        return sb.toString();
    }
}